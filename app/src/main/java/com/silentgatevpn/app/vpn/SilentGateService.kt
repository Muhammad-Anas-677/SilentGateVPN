package com.silentgatevpn.app.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.IpPrefix
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.Process
import androidx.core.app.NotificationCompat
import com.silentgatevpn.app.MainActivity
import com.silentgatevpn.app.R
import io.nekohasekai.libbox.AutoRedirectHandler
import io.nekohasekai.libbox.AutoRedirectSession
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.nekohasekai.libbox.NetworkInterface as BoxInterface
import io.nekohasekai.libbox.Notification as BoxNotification
import java.net.InetAddress
import java.net.NetworkInterface as JavaInterface

/** Simple list-backed StringIterator for the Go side. */
class StringList(private val items: List<String>) : StringIterator {
    private var i = 0
    override fun len(): Int = items.size - i
    override fun hasNext(): Boolean = i < items.size
    override fun next(): String = items[i++]
}

class InterfaceList(private val items: List<BoxInterface>) : NetworkInterfaceIterator {
    private var i = 0
    override fun hasNext(): Boolean = i < items.size
    override fun next(): BoxInterface = items[i++]
}

class SilentGateService : VpnService(), PlatformInterface, CommandServerHandler {

    companion object {
        const val ACTION_START = "com.silentgatevpn.app.START"
        const val ACTION_STOP = "com.silentgatevpn.app.STOP"
        private const val CHANNEL_ID = "silentgate_vpn"
        private const val NOTIFICATION_ID = 17
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var connectJob: Job? = null
    private var monitorJob: Job? = null
    private var server: CommandServer? = null
    private var currentLayer: ConfigBuilder.Layer? = null

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // ---------------------------------------------------------------- lifecycle

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                connectJob?.cancel()
                disconnect()
                stopSelf()
            }
            return START_NOT_STICKY
        }
        startForegroundNow("Connecting…")
        if (connectJob?.isActive != true && VpnState.status.value != Status.CONNECTED) {
            connectJob = scope.launch { connectFlow(skip = null) }
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        scope.launch { connectJob?.cancel(); disconnect(); stopSelf() }
    }

    override fun onDestroy() {
        runCatching { server?.closeService() }
        runCatching { server?.close() }
        server = null
        scope.coroutineContext[Job]?.cancel()
        if (VpnState.status.value != Status.ERROR) VpnState.status.value = Status.DISCONNECTED
        super.onDestroy()
    }

    // ---------------------------------------------------------------- connect flow

    private fun layersFor(mode: ConnectMode): List<ConfigBuilder.Layer> = when (mode) {
        ConnectMode.AUTO -> listOf(
            ConfigBuilder.Layer.WARP, ConfigBuilder.Layer.STEALTH, ConfigBuilder.Layer.LIGHT
        )
        ConnectMode.WARP -> listOf(ConfigBuilder.Layer.WARP)
        ConnectMode.STEALTH -> listOf(ConfigBuilder.Layer.STEALTH)
        ConnectMode.LIGHT -> listOf(ConfigBuilder.Layer.LIGHT)
    }

    private fun overrides() = OverrideOptions().apply {
        setAutoRedirect(false)
        setIncludePackage(StringList(emptyList()))
        setExcludePackage(StringList(emptyList()))
    }

    private suspend fun connectFlow(skip: ConfigBuilder.Layer?) {
        monitorJob?.cancel()
        VpnState.status.value = Status.CONNECTING
        VpnState.lastFailure.value = emptyList()
        VpnState.resetSession()
        val s = SettingsStore.value
        var order = layersFor(s.mode)
        if (skip != null) order = order.filter { it != skip } + order.filter { it == skip }
        try {
            if (server == null) {
                server = Libbox.newCommandServer(this, this).also { it.start() }
            }
            val reasons = mutableListOf<String>()
            for (layer in order) {
                currentLayer = layer
                VpnState.layer.value = layer
                VpnState.message.value = "Trying ${layer.title}…"
                updateNotification("Trying ${layer.title}…")
                var started = false
                var okVariant = 0
                var lastError = ""
                for (variant in 0 until ConfigBuilder.variants(layer)) {
                    val cfg = try {
                        ConfigBuilder.build(layer, s, ProfileStore.active(), variant)
                    } catch (e: Exception) {
                        lastError = e.message ?: "config error"
                        VpnState.log("Skipped ${layer.title}: $lastError")
                        break
                    }
                    try {
                        server!!.startOrReloadService(cfg, overrides())
                        started = true
                        okVariant = variant
                        if (variant > 0) VpnState.log("${layer.title}: running with reduced options (variant $variant)")
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e.message ?: e.toString()
                        VpnState.log("${layer.title} start error (variant $variant): $lastError")
                    }
                }
                if (!started) {
                    reasons += "${layer.title}: ${lastError.take(140)}"
                    continue
                }
                var result = Net.waitForTunnel(layer.timeoutMs)
                if (result == Net.TunnelResult.DNS_BROKEN) {
                    // Tunnel carries traffic but names do not resolve (resolver blocked by the ISP): rotate DNS provider.
                    for (d in DnsProvider.entries.filter { it != s.dns }) {
                        VpnState.message.value = "DNS blocked, trying ${d.label}…"
                        VpnState.log("${layer.title}: DNS broken with ${s.dns.label}, trying ${d.label}")
                        val ok = runCatching {
                            server!!.startOrReloadService(ConfigBuilder.build(layer, s.copy(dns = d), ProfileStore.active(), okVariant), overrides())
                        }.isSuccess
                        if (!ok) continue
                        if (Net.waitForTunnel(15_000) == Net.TunnelResult.OK) {
                            SettingsStore.update { it.copy(dns = d) }
                            result = Net.TunnelResult.OK
                            break
                        }
                    }
                }
                if (result == Net.TunnelResult.OK) {
                    onConnected(layer)
                    return
                }
                if (result == Net.TunnelResult.DNS_BROKEN) {
                    reasons += "${layer.title}: tunnel works but every DNS provider is blocked on this network"
                    continue
                }
                VpnState.log("${layer.title}: started but no traffic, falling back")
                reasons += "${layer.title}: started but no traffic (network may block it)"
            }
            VpnState.lastFailure.value = reasons
            fail("No layer could connect. See details below.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            fail(e.message ?: "Unknown error")
        }
    }

    private fun onConnected(layer: ConfigBuilder.Layer) {
        VpnState.status.value = Status.CONNECTED
        VpnState.message.value = "Protected via ${layer.title}"
        updateNotification("Connected · ${layer.title}")
        refreshAlwaysOn()
        scope.launch {
            runCatching { VpnState.exit.value = Net.exitInfo() }
        }
        startMonitor(layer)
    }

    private fun startMonitor(layer: ConfigBuilder.Layer) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            val uid = Process.myUid()
            fun rx() = TrafficStats.getUidRxBytes(uid).let { if (it < 0) TrafficStats.getTotalRxBytes() else it }
            fun tx() = TrafficStats.getUidTxBytes(uid).let { if (it < 0) TrafficStats.getTotalTxBytes() else it }
            val rx0 = rx(); val tx0 = tx()
            var lastRx = rx0; var lastTx = tx0
            var tick = 0
            var failures = 0
            while (true) {
                delay(1000)
                val r = rx(); val t = tx()
                VpnState.rxSpeed.value = (r - lastRx).coerceAtLeast(0)
                VpnState.txSpeed.value = (t - lastTx).coerceAtLeast(0)
                VpnState.sessionRx.value = (r - rx0).coerceAtLeast(0)
                VpnState.sessionTx.value = (t - tx0).coerceAtLeast(0)
                lastRx = r; lastTx = t
                tick++
                if (tick % 15 == 0) {
                    val ok = Net.probe(6000)
                    failures = if (ok) 0 else failures + 1
                    if (failures >= 3 && SettingsStore.value.mode == ConnectMode.AUTO) {
                        VpnState.log("Tunnel stalled on ${layer.title}; switching layer")
                        connectJob = scope.launch { connectFlow(skip = layer) }
                        return@launch
                    }
                }
            }
        }
    }

    private fun fail(message: String) {
        VpnState.message.value = message
        VpnState.status.value = Status.ERROR
        VpnState.log("Error: $message")
        runCatching { server?.closeService() }
        runCatching { server?.close() }
        server = null
        stopForegroundCompat()
        stopSelf()
    }

    private suspend fun disconnect() {
        VpnState.status.value = Status.DISCONNECTING
        monitorJob?.cancel()
        runCatching { server?.closeService() }
        runCatching { server?.close() }
        server = null
        VpnState.resetSession()
        VpnState.layer.value = null
        VpnState.message.value = ""
        VpnState.status.value = Status.DISCONNECTED
        stopForegroundCompat()
    }

    private fun refreshAlwaysOn() {
        if (Build.VERSION.SDK_INT >= 29) {
            VpnState.alwaysOn.value = isAlwaysOn
            VpnState.lockdown.value = isLockdownEnabled
        }
    }

    // ---------------------------------------------------------------- notification

    private fun buildNotification(text: String) : android.app.Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "VPN status", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, SilentGateService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle("SilentGate VPN")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "Disconnect", stop)
            .build()
    }

    private fun startForegroundNow(text: String) {
        val n = buildNotification(text)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // ---------------------------------------------------------------- CommandServerHandler

    override fun serviceStop() {
        scope.launch { connectJob?.cancel(); disconnect(); stopSelf() }
    }

    override fun serviceReload() {
        VpnState.log("Core requested reload")
    }

    override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus()
    override fun setSystemProxyEnabled(enabled: Boolean) {}
    override fun triggerNativeCrash() {}
    override fun writeDebugMessage(message: String?) {
        if (!message.isNullOrBlank()) VpnState.log(message)
    }
    override fun connectSSHAgent(): Int = throw UnsupportedOperationException("SSH agent not supported")

    // ---------------------------------------------------------------- PlatformInterface

    override fun localDNSTransport(): LocalDNSTransport? = null
    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        protect(fd)
    }

    override fun openTun(options: TunOptions): Int {
        if (VpnService.prepare(this) != null) throw IllegalStateException("VPN permission not granted")
        val b = Builder().setSession("SilentGate VPN").setMtu(options.getMTU())
        if (Build.VERSION.SDK_INT >= 29) b.setMetered(false)

        var has4 = false
        var has6 = false
        options.getInet4Address().let { while (it.hasNext()) { val p = it.next(); b.addAddress(p.address(), p.prefix()); has4 = true } }
        options.getInet6Address().let { while (it.hasNext()) { val p = it.next(); b.addAddress(p.address(), p.prefix()); has6 = true } }

        if (options.getAutoRoute()) {
            options.getDNSServerAddress().let { if (it.hasNext()) b.addDnsServer(it.next()) }

            var any4 = false
            options.getInet4RouteAddress().let { while (it.hasNext()) { val p = it.next(); b.addRoute(p.address(), p.prefix()); any4 = true } }
            if (!any4 && has4) b.addRoute("0.0.0.0", 0)
            var any6 = false
            options.getInet6RouteAddress().let { while (it.hasNext()) { val p = it.next(); b.addRoute(p.address(), p.prefix()); any6 = true } }
            if (!any6 && has6) b.addRoute("::", 0)

            if (Build.VERSION.SDK_INT >= 33) {
                options.getInet4RouteExcludeAddress().let {
                    while (it.hasNext()) { val p = it.next(); b.excludeRoute(IpPrefix(InetAddress.getByName(p.address()), p.prefix())) }
                }
                options.getInet6RouteExcludeAddress().let {
                    while (it.hasNext()) { val p = it.next(); b.excludeRoute(IpPrefix(InetAddress.getByName(p.address()), p.prefix())) }
                }
            }

            val include = options.getIncludePackage()
            val exclude = options.getExcludePackage()
            if (include.hasNext()) {
                while (include.hasNext()) {
                    runCatching { b.addAllowedApplication(include.next()) }
                }
            } else {
                while (exclude.hasNext()) {
                    runCatching { b.addDisallowedApplication(exclude.next()) }
                }
            }
        }

        val pfd = b.establish() ?: throw IllegalStateException("Could not establish the VPN interface")
        return pfd.detachFd()
    }

    override fun findConnectionOwner(
        ipProtocol: Int, sourceAddress: String?, sourcePort: Int, destinationAddress: String?, destinationPort: Int
    ): ConnectionOwner = throw UnsupportedOperationException("not supported")

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        val cm = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val cb = object : ConnectivityManager.NetworkCallback() {
            private fun push(network: Network) {
                val name = cm.getLinkProperties(network)?.interfaceName ?: return
                val index = runCatching { JavaInterface.getByName(name)?.index ?: 0 }.getOrDefault(0)
                listener.updateDefaultInterface(name, index, false, false)
            }
            override fun onAvailable(network: Network) = push(network)
            override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) = push(network)
            override fun onLost(network: Network) {
                listener.updateDefaultInterface("", -1, false, false)
            }
        }
        networkCallback = cb
        if (Build.VERSION.SDK_INT >= 31) {
            cm.registerBestMatchingNetworkCallback(request, cb, android.os.Handler(mainLooper))
        } else {
            cm.requestNetwork(request, cb)
        }
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        val cb = networkCallback ?: return
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(cb) }
        networkCallback = null
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val result = ArrayList<BoxInterface>()
        val all = runCatching { JavaInterface.getNetworkInterfaces()?.toList() }.getOrNull().orEmpty()
        for (ni in all) {
            if (!ni.isUp) continue
            val box = BoxInterface()
            box.setIndex(ni.index)
            box.setMTU(ni.mtu)
            box.setName(ni.name)
            box.setAddresses(StringList(ni.interfaceAddresses.map {
                it.address.hostAddress!!.substringBefore('%') + "/" + it.networkPrefixLength
            }))
            var flags = 0x1 or 0x40 // IFF_UP | IFF_RUNNING
            if (ni.isLoopback) flags = flags or 0x8
            if (ni.isPointToPoint) flags = flags or 0x10
            if (ni.supportsMulticast()) flags = flags or 0x1000
            box.setFlags(flags)
            val type = when {
                ni.name.startsWith("wlan") -> 0
                ni.name.startsWith("rmnet") || ni.name.startsWith("ccmni") || ni.name.startsWith("v4-rmnet") -> 1
                ni.name.startsWith("eth") -> 2
                else -> 3
            }
            box.setType(type)
            box.setDNSServer(StringList(emptyList()))
            box.setDNSSearchDomain(StringList(emptyList()))
            box.setGateway(StringList(emptyList()))
            box.setMetered(false)
            result += box
        }
        return InterfaceList(result)
    }

    override fun underNetworkExtension(): Boolean = false
    override fun includeAllNetworks(): Boolean = false
    override fun readWIFIState(): WIFIState? = null
    override fun systemCertificates(): StringIterator = StringList(emptyList())
    override fun clearDNSCache() {}
    override fun sendNotification(notification: BoxNotification?) {}
    override fun cancelNotification(identifier: String?, typeID: Int) {}
    override fun startNeighborMonitor(listener: NeighborUpdateListener?) {}
    override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {}
    override fun registerMyInterface(name: String?) {}

    override fun usePlatformShell(): Boolean = false
    override fun checkPlatformShell() {}
    override fun openShellSession(
        user: PlatformUser?, command: String?, environ: StringIterator?, term: String?, rows: Int, cols: Int
    ): ShellSession = throw UnsupportedOperationException("shell not supported")
    override fun lookupUser(username: String?): PlatformUser = throw UnsupportedOperationException("not supported")
    override fun lookupSFTPServer(): String = throw UnsupportedOperationException("not supported")
    override fun readSystemSSHHostKey(): String = throw UnsupportedOperationException("not supported")
    override fun tailscaleHostname(): String = ""

    override fun usePlatformBridge(): Boolean = false
    override fun createBridge(options: BridgeOptions?): BridgeSession = throw UnsupportedOperationException("not supported")
    override fun usePlatformAutoRedirect(): Boolean = false
    override fun createAutoRedirect(options: ByteArray?, handler: AutoRedirectHandler?): AutoRedirectSession =
        throw UnsupportedOperationException("not supported")
}

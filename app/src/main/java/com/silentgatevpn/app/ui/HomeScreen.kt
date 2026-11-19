package com.silentgatevpn.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silentgatevpn.app.vpn.ConnectMode
import com.silentgatevpn.app.vpn.SettingsStore
import com.silentgatevpn.app.vpn.Status
import com.silentgatevpn.app.vpn.VpnState
import com.silentgatevpn.app.vpn.formatBytes

@Composable
fun HomeScreen(onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val status by VpnState.status.collectAsState()
    val layer by VpnState.layer.collectAsState()
    val message by VpnState.message.collectAsState()
    val exit by VpnState.exit.collectAsState()
    val rx by VpnState.rxSpeed.collectAsState()
    val tx by VpnState.txSpeed.collectAsState()
    val sRx by VpnState.sessionRx.collectAsState()
    val sTx by VpnState.sessionTx.collectAsState()
    val settings by SettingsStore.flow.collectAsState()
    val alwaysOn by VpnState.alwaysOn.collectAsState()
    val lockdown by VpnState.lockdown.collectAsState()

    val connected = status == Status.CONNECTED
    val busy = status == Status.CONNECTING || status == Status.DISCONNECTING

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = SgGreen, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(10.dp))
            Text("SilentGate VPN", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(28.dp))
        ConnectButton(status = status, onClick = {
            when (status) {
                Status.DISCONNECTED, Status.ERROR -> onConnect()
                Status.CONNECTED, Status.CONNECTING -> onDisconnect()
                else -> Unit
            }
        })
        Spacer(Modifier.height(20.dp))

        val headline = when (status) {
            Status.DISCONNECTED -> "Not protected"
            Status.CONNECTING -> "Connecting…"
            Status.CONNECTED -> "Protected"
            Status.DISCONNECTING -> "Disconnecting…"
            Status.ERROR -> "Connection failed"
        }
        Text(
            headline, style = MaterialTheme.typography.headlineMedium,
            color = when (status) {
                Status.CONNECTED -> SgGreen
                Status.ERROR -> SgDanger
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        val sub = when {
            message.isNotEmpty() -> message
            status == Status.DISCONNECTED -> "Tap the button to connect"
            else -> ""
        }
        if (sub.isNotEmpty()) {
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }

        if (status == Status.ERROR) {
            val reasons by VpnState.lastFailure.collectAsState()
            val log by VpnState.log.collectAsState()
            val clipboard = LocalClipboardManager.current
            Spacer(Modifier.height(14.dp))
            SgCardBox(container = SgDangerBg, border = SgDanger) {
                Text("Why it failed", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFB4AB))
                (if (reasons.isEmpty()) log.takeLast(4) else reasons).forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB4AB), modifier = Modifier.padding(top = 4.dp))
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(log.joinToString("\n"))) },
                    modifier = Modifier.padding(top = 10.dp),
                    border = BorderStroke(1.dp, SgDanger)
                ) { Text("Copy full log", color = Color(0xFFFFB4AB)) }
            }
        }

        Spacer(Modifier.height(20.dp))
        ChoiceChips(
            options = ConnectMode.entries, selected = settings.mode, label = { it.label },
            enabled = status == Status.DISCONNECTED || status == Status.ERROR
        ) { m -> SettingsStore.update { it.copy(mode = m) } }
        Text(settings.mode.hint, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(20.dp))
        SgCardBox {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(Icons.Rounded.ArrowDownward, "Download", "${formatBytes(rx)}/s", formatBytes(sRx))
                Stat(Icons.Rounded.ArrowUpward, "Upload", "${formatBytes(tx)}/s", formatBytes(sTx))
            }
            if (connected && layer != null) {
                Spacer(Modifier.height(12.dp))
                Text("Active layer: ${layer!!.title}", style = MaterialTheme.typography.bodyMedium, color = SgGreenSoft)
            }
        }

        if (connected) {
            Spacer(Modifier.height(12.dp))
            val e = exit
            SgCardBox {
                Text("Exit", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (e == null) {
                    Text("Checking exit location…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    InfoLine("IP", e.ip)
                    InfoLine("Location", listOf(e.city, e.region, e.country).filter { it.isNotEmpty() }.joinToString(", "))
                    if (e.colo.isNotEmpty()) InfoLine("Cloudflare edge", e.colo)
                    if (e.pingMs >= 0) InfoLine("Latency", "${e.pingMs} ms")
                    e.warpOn?.let { InfoLine("WARP", if (it) "On" else "Off (other layer)") }
                }
            }
            if (e?.restricted == true) {
                Spacer(Modifier.height(12.dp))
                SgCardBox(container = SgDangerBg, border = SgDanger) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = SgDanger)
                        Spacer(Modifier.size(10.dp))
                        Text("Exit is in a region with age-verification rules", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFB4AB))
                    }
                    Text(
                        "Your exit location (${e.region.ifEmpty { e.country }}) is on the restricted list. Reconnect, or switch layer in Profiles/Settings.",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB4AB), modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SgCardBox(container = if (alwaysOn && lockdown) SgCard else SgWarnBg, border = if (alwaysOn && lockdown) MaterialTheme.colorScheme.outlineVariant else SgOrange) {
                Text("Kill switch", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (alwaysOn && lockdown) "Always-on VPN and Block connections without VPN are active."
                    else "For a real kill switch, turn on Always-on VPN and Block connections without VPN in Android settings (Settings tab has a shortcut).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SgCardBox {
            Text("Tip", style = MaterialTheme.typography.titleMedium)
            Text(
                "If a site already showed an age-verification banner on another connection, clear its cookies or use Incognito after connecting.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        MadeByAnas()
    }
}

@Composable
private fun ConnectButton(status: Status, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.25f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse"
    )
    val connected = status == Status.CONNECTED
    val connecting = status == Status.CONNECTING
    val ringColor = when (status) {
        Status.CONNECTED -> SgGreen
        Status.ERROR -> SgDanger
        Status.CONNECTING -> SgGreenSoft
        else -> Color(0xFF3A3A3A)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
        Box(
            Modifier.size(210.dp).alpha(if (connecting) pulse else if (connected) 0.25f else 0f)
                .clip(CircleShape).background(SgGreen)
        )
        Box(
            Modifier.size(172.dp).clip(CircleShape)
                .background(if (connected) SgGreen else Color(0xFF0E0E0E))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.PowerSettingsNew, contentDescription = "Connect",
                tint = if (connected) Color.Black else ringColor, modifier = Modifier.size(76.dp)
            )
        }
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, speed: String, total: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SgGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(speed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Session $total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp))
    }
}

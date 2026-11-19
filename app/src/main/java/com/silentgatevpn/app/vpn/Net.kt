package com.silentgatevpn.app.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.random.Random

data class Trace(val ip: String, val loc: String, val colo: String, val warp: Boolean, val rttMs: Long)

object Net {

    // Regions where online-safety / age-verification rules are commonly enforced. Laws change; keep this list editable.
    private val RESTRICTED_COUNTRIES = setOf("GB", "AU", "FR", "DE", "IT", "ES", "IE")
    private val RESTRICTED_US_STATES = setOf(
        "AL", "AZ", "AR", "FL", "GA", "ID", "IN", "KS", "KY", "LA", "MO", "MS", "MT",
        "NE", "NC", "ND", "OH", "OK", "SC", "SD", "TN", "TX", "UT", "VA", "WY"
    )

    fun isRestricted(countryCode: String, regionCode: String): Boolean =
        countryCode.uppercase() in RESTRICTED_COUNTRIES ||
            (countryCode.equals("US", true) && regionCode.uppercase() in RESTRICTED_US_STATES)

    suspend fun http(url: String, timeoutMs: Int = 8000): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.requestMethod = "GET"
            c.setRequestProperty("User-Agent", "SilentGateVPN/1.0")
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    suspend fun cfTrace(timeoutMs: Int = 8000): Trace {
        val t0 = System.currentTimeMillis()
        val body = http("https://1.1.1.1/cdn-cgi/trace", timeoutMs)
        val rtt = System.currentTimeMillis() - t0
        val map = body.lineSequence().mapNotNull {
            val i = it.indexOf('=')
            if (i > 0) it.substring(0, i) to it.substring(i + 1) else null
        }.toMap()
        val ip = map["ip"] ?: error("no ip in trace")
        return Trace(ip, map["loc"] ?: "", map["colo"] ?: "", (map["warp"] ?: "off") != "off", rtt)
    }

    // Hostname probes: succeed only if DNS works through the tunnel AND traffic flows.
    private val HOST_PROBES = listOf(
        "https://cp.cloudflare.com/generate_204",
        "https://www.gstatic.com/generate_204",
        "https://connectivitycheck.gstatic.com/generate_204"
    )
    // IP probes: need no DNS. If these pass while host probes fail, the tunnel is fine but DNS is broken.
    private val IP_PROBES = listOf(
        "https://1.1.1.1/cdn-cgi/trace",
        "https://8.8.8.8/resolve?name=example.com&type=A"
    )

    private suspend fun anyOk(urls: List<String>, timeoutMs: Int): Boolean = coroutineScope {
        urls.map { u -> async(Dispatchers.IO) { runCatching { http(u, timeoutMs) }.isSuccess } }.awaitAll().any { it }
    }

    /** True when real websites (by name) are reachable through the tunnel. */
    suspend fun probe(timeoutMs: Int = 6000): Boolean = anyOk(HOST_PROBES, timeoutMs)

    enum class TunnelResult { OK, DNS_BROKEN, NO_TRAFFIC }

    /** Polls until names resolve and traffic flows, DNS is detected as broken, or the time runs out. */
    suspend fun waitForTunnel(timeoutMs: Long): TunnelResult {
        val end = System.currentTimeMillis() + timeoutMs
        delay(1500)
        var dnsOnly = 0
        while (System.currentTimeMillis() < end) {
            if (probe(6000)) return TunnelResult.OK
            if (anyOk(IP_PROBES, 5000)) { if (++dnsOnly >= 2) return TunnelResult.DNS_BROKEN } else dnsOnly = 0
            delay(1000)
        }
        return TunnelResult.NO_TRAFFIC
    }

    suspend fun pingMedian(count: Int = 3): Long {
        val list = mutableListOf<Long>()
        repeat(count) { runCatching { list += cfTrace(5000).rttMs } }
        return if (list.isEmpty()) -1 else list.sorted()[list.size / 2]
    }

    suspend fun exitInfo(): ExitInfo {
        val trace = cfTrace()
        var country = ""; var cc = trace.loc; var region = ""; var rc = ""; var city = ""
        runCatching {
            val j = JSONObject(http("https://ipwho.is/"))
            if (j.optBoolean("success", true)) {
                country = j.optString("country"); cc = j.optString("country_code", cc)
                region = j.optString("region"); rc = j.optString("region_code"); city = j.optString("city")
            }
        }
        val ping = pingMedian(3)
        return ExitInfo(trace.ip, country.ifEmpty { cc }, cc, region, rc, city, trace.warp, trace.colo, ping, isRestricted(cc, rc))
    }

    data class DnsLeak(val resolverIp: String, val resolverGeo: String, val leaking: Boolean)

    suspend fun dnsLeak(): DnsLeak {
        val label = (1..12).map { "abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)] }.joinToString("")
        val j = JSONObject(http("https://$label.edns.ip-api.com/json", 10000))
        val d = j.optJSONObject("dns") ?: error("no dns data")
        val geo = d.optString("geo")
        val ip = d.optString("ip")
        // A resolver that belongs to a local Pakistani ISP means the query left the tunnel.
        val leaking = geo.contains("Pakistan", true)
        return DnsLeak(ip, geo, leaking)
    }

    /** Returns the IPv6 address the internet sees, or null when IPv6 is unreachable (expected while IPv6 is blocked). */
    suspend fun ipv6Probe(): String? = runCatching {
        JSONObject(http("https://api6.ipify.org?format=json", 5000)).optString("ip").ifEmpty { null }
    }.getOrNull()

    data class Candidate(val ip: String, val ms: Long)

    /** TCP/443 reachability scan of Cloudflare edge ranges, useful to pick a clean address for a Worker profile. */
    suspend fun scanCloudflare(samplesPerRange: Int = 14, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<Candidate> {
        val ranges = listOf("188.114.96", "188.114.97", "188.114.98", "188.114.99", "162.159.192", "162.159.195", "104.16.0", "172.67.0")
        val ips = ranges.flatMap { r -> (1..254).shuffled().take(samplesPerRange).map { "$r.$it" } }
        val sem = Semaphore(24)
        var done = 0
        return coroutineScope {
            ips.map { ip ->
                async(Dispatchers.IO) {
                    sem.withPermit {
                        val t0 = System.currentTimeMillis()
                        val ok = runCatching {
                            Socket().use { it.connect(InetSocketAddress(ip, 443), 1500) }
                        }.isSuccess
                        synchronized(this@Net) { done++; onProgress(done, ips.size) }
                        if (ok) Candidate(ip, System.currentTimeMillis() - t0) else null
                    }
                }
            }.awaitAll().filterNotNull().sortedBy { it.ms }.take(8)
        }
    }

    data class SiteStep(val title: String, val ok: Boolean?, val detail: String)

    /** Step-by-step reachability test of one site through the current network path (DNS, TCP 443, HTTPS). */
    suspend fun siteTest(input: String): List<SiteStep> = withContext(Dispatchers.IO) {
        val out = mutableListOf<SiteStep>()
        val host = input.trim().removePrefix("https://").removePrefix("http://")
            .substringBefore('/').substringBefore('?').trim()
        if (host.isEmpty() || host.any { it.isWhitespace() }) {
            out += SiteStep("Input", false, "Enter a domain like example.com")
            return@withContext out
        }

        // 1. DNS
        val d0 = System.currentTimeMillis()
        val addrs = try {
            InetAddress.getAllByName(host).toList()
        } catch (e: Exception) {
            out += SiteStep("DNS", false, "Could not resolve $host. The domain is down or DNS is blocked.")
            return@withContext out
        }
        val bogus = addrs.any { it.isAnyLocalAddress || it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress }
        out += SiteStep(
            "DNS", !bogus,
            (if (bogus) "Answer is a fake/private address (DNS poisoned): " else "Resolved in ${System.currentTimeMillis() - d0} ms: ") +
                addrs.take(3).joinToString { it.hostAddress ?: "" }
        )
        if (bogus) return@withContext out

        // 2. TCP 443
        val target = addrs.firstOrNull { it is Inet4Address } ?: addrs.first()
        val t0 = System.currentTimeMillis()
        try {
            Socket().use { it.connect(InetSocketAddress(target, 443), 7000) }
            out += SiteStep("TCP 443", true, "Connected to ${target.hostAddress} in ${System.currentTimeMillis() - t0} ms")
        } catch (e: SocketTimeoutException) {
            out += SiteStep("TCP 443", false, "Timed out. Packets to ${target.hostAddress} are dropped (IP blocked on this path).")
            return@withContext out
        } catch (e: Exception) {
            out += SiteStep("TCP 443", false, "Connection refused or reset: ${e.message ?: e.javaClass.simpleName}")
            return@withContext out
        }

        // 3. HTTPS request with a browser-like user agent
        val c = URL("https://$host/").openConnection() as HttpURLConnection
        try {
            c.instanceFollowRedirects = false
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
            c.setRequestProperty("Accept", "text/html,*/*")
            val code = c.responseCode
            val server = c.getHeaderField("Server") ?: ""
            val mitigated = c.getHeaderField("cf-mitigated") ?: ""
            val loc = c.getHeaderField("Location") ?: ""
            val stream = if (code >= 400) c.errorStream else c.inputStream
            val body = runCatching {
                val buf = ByteArray(4096)
                val n = stream?.use { it.read(buf) } ?: 0
                String(buf, 0, n.coerceAtLeast(0)).lowercase()
            }.getOrDefault("")
            val info = "HTTP $code" + (if (server.isNotEmpty()) " · server: $server" else "") +
                (if (loc.isNotEmpty()) " · redirect to ${loc.take(60)}" else "")
            val verdict: Pair<Boolean?, String> = when {
                mitigated.equals("challenge", true) || "just a moment" in body || "captcha" in body ->
                    false to "$info. The site shows a Cloudflare/captcha challenge to this IP. It often blocks VPN/WARP addresses, so the page may look blank."
                body.contains("pakistan telecommunication") || body.contains("pta") && body.contains("block") ->
                    false to "$info. This is an ISP/PTA block page. Traffic is not going through the tunnel."
                code == 451 -> false to "$info. Blocked for legal reasons (451)."
                code == 403 -> false to "$info. The site refuses this IP address (it blocks the VPN exit), not your ISP."
                code in 200..399 -> true to "$info. The site answers normally. If the browser still shows nothing, try Incognito or turn off QUIC in Chrome."
                else -> null to info
            }
            out += SiteStep("HTTPS", verdict.first, verdict.second)
        } catch (e: SocketTimeoutException) {
            out += SiteStep("HTTPS", false, "TCP connects but no reply: the connection is silently dropped after the handshake (DPI or MTU problem).")
        } catch (e: SSLException) {
            out += SiteStep("HTTPS", false, "TLS handshake failed (${e.message?.take(80)}). The connection is being reset or tampered with.")
        } catch (e: Exception) {
            out += SiteStep("HTTPS", false, "Request failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            c.disconnect()
        }
        out
    }
}

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

    private val PROBES = listOf(
        "https://1.1.1.1/cdn-cgi/trace",
        "https://cp.cloudflare.com/generate_204",
        "https://www.gstatic.com/generate_204",
        "https://connectivitycheck.gstatic.com/generate_204"
    )

    /** True as soon as ANY probe answers. One blocked host (common on some ISPs) no longer fails the whole connection. */
    suspend fun probe(timeoutMs: Int = 6000): Boolean = coroutineScope {
        val jobs = PROBES.map { u -> async(Dispatchers.IO) { runCatching { http(u, timeoutMs) }.isSuccess } }
        var ok = false
        for (j in jobs) { if (j.await()) { ok = true; break } }
        jobs.forEach { it.cancel() }
        ok
    }

    /** Polls until traffic flows through the tunnel or the time runs out. */
    suspend fun waitForTunnel(timeoutMs: Long): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        delay(1500)
        while (System.currentTimeMillis() < end) {
            if (probe(6000)) return true
            delay(1500)
        }
        return false
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
}

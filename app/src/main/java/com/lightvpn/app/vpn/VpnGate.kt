package com.lightvpn.app.vpn

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the public VPN Gate server list (volunteer OpenVPN servers) over HTTPS only.
 * Must run off the main thread.
 */
object VpnGate {

    data class Server(val code: String, val country: String, val score: Long, val config: String)

    private const val API_URL = "https://www.vpngate.net/api/iphone/"
    private const val MAX_BYTES = 8_000_000
    private const val MAX_CONFIG = 40_000

    private val CODE_RE = Regex("^[A-Z]{2}$")

    // Directives that must never come from a volunteer-supplied config.
    // OpenVPN also accepts the command-line style "--directive" inside config files, so allow an optional "--".
    private val BAD_DIRECTIVE = Regex(
        "^\\s*(?:--)?(script-security|up|down|up-restart|route-up|route-pre-down|ipchange|learn-address|" +
            "client-connect|client-disconnect|auth-user-pass-verify|tls-verify|plugin|setenv|setenv-safe|" +
            "management|writepid|log|log-append|cd|chroot|daemon)\\b",
        RegexOption.IGNORE_CASE
    )

    fun fetch(): List<Server> {
        val c = URL(API_URL).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 20_000
            c.readTimeout = 30_000
            c.setRequestProperty("Accept", "text/csv,text/plain,*/*")
            if (c.responseCode !in 200..299) throw IOException("VPN Gate HTTP ${c.responseCode}")
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            c.inputStream.use { ins ->
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) throw IOException("VPN Gate list too large")
                    out.write(buf, 0, n)
                }
            }
            return out.toString("UTF-8").lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("*") && !it.startsWith("#") }
                .mapNotNull { parse(it) }
                .toList()
        } finally {
            c.disconnect()
        }
    }

    // CSV: HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,
    //      TotalUsers,TotalTraffic,LogType,Operator,Message,OpenVPN_ConfigData_Base64
    private fun parse(line: String): Server? {
        val f = line.split(",")
        if (f.size < 15) return null
        val code = f[6].trim().uppercase()
        if (!CODE_RE.matches(code)) return null
        val score = f[2].trim().toLongOrNull() ?: 0L
        val config = runCatching {
            String(Base64.decode(f.last().trim(), Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull() ?: return null
        if (config.length > MAX_CONFIG || !config.contains("remote ")) return null
        if (config.lineSequence().any { BAD_DIRECTIVE.containsMatchIn(it) }) return null
        return Server(code, f[5].trim().ifEmpty { code }, score, config)
    }
}

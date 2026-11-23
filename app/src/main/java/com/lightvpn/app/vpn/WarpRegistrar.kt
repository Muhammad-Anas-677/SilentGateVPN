package com.lightvpn.app.vpn

import android.net.Uri
import com.wireguard.crypto.KeyPair
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

/**
 * Registers a free Cloudflare WARP device and builds a WireGuard config from the response.
 * Unofficial API (same one used by the open-source "wgcf" tool) - it can change or stop working.
 * Must run off the main thread.
 */
object WarpRegistrar {

    class WarpException(message: String) : Exception(message)

    private const val BASE = "https://api.cloudflareclient.com/v0a1922"
    private const val ENDPOINT = "engage.cloudflareclient.com:2408"

    private val KEY_RE = Regex("^[A-Za-z0-9+/]{43}=$")
    private val V4_RE = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")
    private val V6_RE = Regex("^[0-9a-fA-F:]{2,45}$")

    // The WARP API rejects TLS 1.3, so force TLS 1.2.
    private val tls12 by lazy {
        SSLContext.getInstance("TLSv1.2").apply { init(null, null, null) }.socketFactory
    }

    fun register(): String {
        val keys = KeyPair()
        val body = JSONObject()
            .put("key", keys.publicKey.toBase64())
            .put("install_id", "")
            .put("fcm_token", "")
            .put("tos", nowIso())
            .put("type", "Android")
            .put("model", "PC")
            .put("locale", "en_US")
            .toString()

        val reg = JSONObject(call("POST", "$BASE/reg", body, null))
        val id = reg.optString("id")
        val token = reg.optString("token")

        var cfg = reg.optJSONObject("config")
        if ((cfg == null || !cfg.has("peers")) && id.isNotEmpty() && token.isNotEmpty()) {
            cfg = JSONObject(call("GET", "$BASE/reg/${Uri.encode(id)}", null, token)).optJSONObject("config")
        }
        if (cfg == null) throw WarpException("WARP setup failed (bad response)")

        val peerKey = cfg.getJSONArray("peers").getJSONObject(0).getString("public_key")
        val addrs = cfg.getJSONObject("interface").getJSONObject("addresses")
        val v4 = addrs.getString("v4")
        val v6 = addrs.optString("v6", "")

        if (!KEY_RE.matches(peerKey) || !V4_RE.matches(v4) || (v6.isNotEmpty() && !V6_RE.matches(v6))) {
            throw WarpException("WARP setup failed (unexpected data)")
        }

        val address = if (v6.isNotEmpty()) "$v4/32, $v6/128" else "$v4/32"
        return """
            [Interface]
            PrivateKey = ${keys.privateKey.toBase64()}
            Address = $address
            DNS = 1.1.1.1, 1.0.0.1, 2606:4700:4700::1111
            MTU = 1280

            [Peer]
            PublicKey = $peerKey
            AllowedIPs = 0.0.0.0/0, ::/0
            Endpoint = $ENDPOINT
        """.trimIndent()
    }

    private fun call(method: String, url: String, body: String?, token: String?): String {
        val c = URL(url).openConnection() as HttpsURLConnection
        try {
            c.sslSocketFactory = tls12
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 15_000
            c.setRequestProperty("User-Agent", "okhttp/3.12.1")
            c.setRequestProperty("CF-Client-Version", "a-6.3-1922")
            c.setRequestProperty("Accept", "application/json")
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            if (code !in 200..299) throw WarpException("WARP setup failed (HTTP $code)")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}

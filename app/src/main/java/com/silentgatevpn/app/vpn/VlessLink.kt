package com.silentgatevpn.app.vpn

import android.net.Uri
import org.json.JSONObject

/** Minimal vless:// link parser (ws/tcp/grpc + tls/none). */
data class VlessLink(
    val name: String,
    val uuid: String,
    val host: String,
    val port: Int,
    val network: String,
    val security: String,
    val sni: String,
    val wsHost: String,
    val path: String,
    val fingerprint: String,
    val alpn: List<String>,
    val flow: String,
    val insecure: Boolean,
    val serviceName: String
) {
    fun toOutbound(tag: String, fragment: JSONObject?): JSONObject {
        val o = JSONObject()
        o.put("type", "vless")
        o.put("tag", tag)
        o.put("server", host)
        o.put("server_port", port)
        o.put("uuid", uuid)
        if (flow.isNotEmpty()) o.put("flow", flow)
        o.put("packet_encoding", "xudp")
        if (security == "tls") {
            val tls = JSONObject().put("enabled", true).put("server_name", sni.ifEmpty { host })
            if (insecure) tls.put("insecure", true)
            if (alpn.isNotEmpty()) tls.put("alpn", org.json.JSONArray(alpn))
            if (fingerprint.isNotEmpty()) {
                tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", fingerprint))
            }
            o.put("tls", tls)
        }
        when (network) {
            "ws" -> {
                val ws = JSONObject().put("type", "ws").put("path", path.ifEmpty { "/" })
                val h = wsHost.ifEmpty { sni.ifEmpty { host } }
                ws.put("headers", JSONObject().put("Host", h))
                o.put("transport", ws)
            }
            "grpc" -> o.put("transport", JSONObject().put("type", "grpc").put("service_name", serviceName))
        }
        if (fragment != null) o.put("tls_fragment", fragment)
        o.put("domain_resolver", "dns-bootstrap")
        return o
    }

    companion object {
        fun parse(link: String): VlessLink {
            val u = Uri.parse(link.trim())
            require(u.scheme.equals("vless", true)) { "not a vless link" }
            val uuid = u.userInfo ?: error("missing UUID")
            val host = u.host ?: error("missing host")
            val port = if (u.port > 0) u.port else 443
            fun q(k: String) = u.getQueryParameter(k) ?: ""
            val network = q("type").ifEmpty { "tcp" }
            val security = q("security").ifEmpty { "none" }
            require(security == "tls" || security == "none") { "only tls/none security is supported" }
            require(network == "ws" || network == "tcp" || network == "grpc") { "unsupported transport: $network" }
            return VlessLink(
                name = (u.fragment ?: "").ifEmpty { host },
                uuid = uuid, host = host, port = port, network = network, security = security,
                sni = q("sni").ifEmpty { q("peer") },
                wsHost = q("host"),
                path = q("path"),
                fingerprint = q("fp").ifEmpty { "chrome" },
                alpn = q("alpn").split(",").map { it.trim() }.filter { it.isNotEmpty() },
                flow = q("flow"),
                insecure = q("allowInsecure") == "1" || q("insecure") == "1",
                serviceName = q("serviceName")
            )
        }
    }
}

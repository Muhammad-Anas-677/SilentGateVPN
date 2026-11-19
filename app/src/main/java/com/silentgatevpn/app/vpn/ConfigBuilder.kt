package com.silentgatevpn.app.vpn

import org.json.JSONArray
import org.json.JSONObject

object ConfigBuilder {

    enum class Layer(val title: String, val timeoutMs: Long) {
        WARP("Obfuscated WARP", 45_000),
        STEALTH("Stealth VLESS", 25_000),
        LIGHT("TLS Fragment + DoH", 15_000)
    }

    private val ENDPOINT_TYPES = setOf("wireguard", "awg", "warp")

    private fun obj(vararg p: Pair<String, Any?>) = JSONObject().apply { p.forEach { (k, v) -> put(k, v) } }
    private fun arr(vararg v: Any) = JSONArray().apply { v.forEach { put(it) } }

    private fun fragmentJson(s: AppSettings) = obj(
        "enabled" to true, "size" to s.fragSize, "sleep" to s.fragSleep
    )

    /** How many progressively simpler configs exist for a layer (used when the core rejects an option). */
    fun variants(layer: Layer): Int = when (layer) {
        Layer.WARP -> 3
        Layer.STEALTH -> 2
        Layer.LIGHT -> 2
    }

    /** variant 0 = full config, higher = fewer optional core features (noise, scanner, fragmentation). */
    fun build(layer: Layer, s: AppSettings, profile: Profile?, variant: Int = 0): String {
        val endpoints = JSONArray()
        val outbounds = JSONArray().put(obj("type" to "direct", "tag" to "direct"))

        val mainTag: String = when (layer) {
            Layer.WARP -> {
                val warp = obj("type" to "warp", "tag" to "warp")
                if (variant <= 1) warp.put("mode", s.warpTunnel.core)
                if (variant == 0 && s.noise && (s.warpTunnel == WarpTunnel.AUTO || s.warpTunnel == WarpTunnel.WG)) {
                    warp.put(
                        "noise", obj(
                            "fake_packet" to obj(
                                "enabled" to true, "mode" to "quic", "count" to "2-4", "delay" to "10-30"
                            )
                        )
                    )
                }
                if (variant <= 1 && s.scanner) warp.put("scanner", obj("enabled" to true, "search_noise" to s.searchNoise))
                endpoints.put(warp)
                "warp"
            }
            Layer.STEALTH -> {
                val p = profile ?: error("No Stealth profile. Add one in Profiles.")
                if (p.kind == "vless") {
                    val link = VlessLink.parse(p.data)
                    outbounds.put(link.toOutbound("proxy", if (s.fragmentStealth && variant == 0) fragmentJson(s) else null))
                } else {
                    val o = JSONObject(p.data)
                    o.put("tag", "proxy")
                    if (o.optString("type") in ENDPOINT_TYPES) endpoints.put(o) else {
                        if (!o.has("domain_resolver")) o.put("domain_resolver", "dns-bootstrap")
                        outbounds.put(o)
                    }
                }
                "proxy"
            }
            Layer.LIGHT -> {
                outbounds.put(
                    if (variant == 0) obj("type" to "direct", "tag" to "frag", "tls_fragment" to fragmentJson(s))
                    else obj("type" to "direct", "tag" to "frag")
                )
                "frag"
            }
        }

        // DNS: everything the apps ask is answered over encrypted DNS through the active tunnel.
        val remote = obj(
            "type" to "https", "tag" to "dns-remote", "server" to s.dns.server,
            "server_port" to 443, "path" to "/dns-query", "detour" to mainTag
        )
        s.dns.sni?.let { remote.put("tls", obj("server_name" to it)) }
        val bootstrap = obj(
            "type" to "https", "tag" to "dns-bootstrap", "server" to "1.1.1.1",
            "server_port" to 443, "path" to "/dns-query"
        )
        val dns = obj(
            "servers" to JSONArray().put(remote).put(bootstrap),
            "final" to "dns-remote",
            "strategy" to if (s.ipv6 == Ipv6Mode.BLOCK) "ipv4_only" else "prefer_ipv4"
        )

        val rules = JSONArray()
        rules.put(obj("action" to "sniff"))
        rules.put(obj("protocol" to "dns", "action" to "hijack-dns"))
        if (s.ipv6 == Ipv6Mode.BLOCK) rules.put(obj("ip_version" to 6, "action" to "reject"))
        rules.put(obj("ip_is_private" to true, "outbound" to "direct"))

        val route = obj(
            "rules" to rules,
            "final" to mainTag,
            "auto_detect_interface" to true,
            "default_domain_resolver" to obj("server" to "dns-bootstrap", "strategy" to "ipv4_only")
        )

        // IPv6 address + ::/0 route always go into the TUN so native IPv6 can never bypass the VPN.
        val tun = obj(
            "type" to "tun", "tag" to "tun-in",
            "address" to arr("172.19.0.1/30", "fdfe:dcba:9876::1/126"),
            "mtu" to s.mtu,
            "auto_route" to true,
            "strict_route" to true,
            "stack" to "mixed"
        )
        if (s.splitPackages.isNotEmpty()) {
            when (s.splitMode) {
                SplitMode.EXCLUDE -> tun.put("exclude_package", JSONArray(s.splitPackages.toList()))
                SplitMode.ONLY -> tun.put("include_package", JSONArray(s.splitPackages.toList()))
                SplitMode.OFF -> Unit
            }
        }

        val root = obj(
            "log" to obj("level" to "warn", "timestamp" to true),
            "experimental" to obj("cache_file" to obj("enabled" to true, "store_warp_config" to true)),
            "dns" to dns,
            "inbounds" to JSONArray().put(tun),
            "outbounds" to outbounds,
            "route" to route
        )
        if (endpoints.length() > 0) root.put("endpoints", endpoints)
        return root.toString()
    }
}

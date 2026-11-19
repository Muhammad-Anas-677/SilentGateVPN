package com.silentgatevpn.app.vpn

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ConnectMode(val label: String, val hint: String) {
    AUTO("Auto", "WARP, then Stealth, then Light"),
    WARP("WARP", "Obfuscated Cloudflare WARP only"),
    STEALTH("Stealth", "VLESS over WebSocket + TLS"),
    LIGHT("Light", "TLS fragmentation + encrypted DNS")
}

enum class WarpTunnel(val label: String, val core: String) {
    AUTO("Auto", "auto"),
    WG("WireGuard", "wg"),
    AWG("AmneziaWG", "awg"),
    MASQUE("MASQUE", "masque")
}

enum class DnsProvider(val label: String, val server: String, val sni: String?) {
    CLOUDFLARE("Cloudflare 1.1.1.1", "1.1.1.1", null),
    QUAD9("Quad9 unfiltered", "9.9.9.10", "dns10.quad9.net"),
    ADGUARD("AdGuard unfiltered", "94.140.14.140", "unfiltered.adguard-dns.com")
}

enum class SplitMode(val label: String) { OFF("Off"), EXCLUDE("Bypass selected"), ONLY("Only selected") }
enum class Ipv6Mode(val label: String) { BLOCK("Block (safest)"), TUNNEL("Tunnel") }

const val CONSENT_VERSION = 1

data class AppSettings(
    val consentVersion: Int = 0,
    val consentTime: Long = 0L,
    val mode: ConnectMode = ConnectMode.AUTO,
    val warpTunnel: WarpTunnel = WarpTunnel.AUTO,
    val noise: Boolean = true,
    val scanner: Boolean = true,
    val searchNoise: Boolean = true,
    val dns: DnsProvider = DnsProvider.CLOUDFLARE,
    val ipv6: Ipv6Mode = Ipv6Mode.BLOCK,
    val fragSize: String = "10-30",
    val fragSleep: String = "2-8",
    val fragmentStealth: Boolean = true,
    val splitMode: SplitMode = SplitMode.OFF,
    val splitPackages: Set<String> = emptySet(),
    val activeProfileId: String? = null,
    val mtu: Int = 1400
) {
    val consented: Boolean get() = consentVersion >= CONSENT_VERSION

    fun toJson(): JSONObject = JSONObject().apply {
        put("consentVersion", consentVersion)
        put("consentTime", consentTime)
        put("mode", mode.name)
        put("warpTunnel", warpTunnel.name)
        put("noise", noise)
        put("scanner", scanner)
        put("searchNoise", searchNoise)
        put("dns", dns.name)
        put("ipv6", ipv6.name)
        put("fragSize", fragSize)
        put("fragSleep", fragSleep)
        put("fragmentStealth", fragmentStealth)
        put("splitMode", splitMode.name)
        put("splitPackages", JSONArray(splitPackages.toList()))
        put("activeProfileId", activeProfileId ?: JSONObject.NULL)
        put("mtu", mtu)
    }

    companion object {
        private inline fun <reified E : Enum<E>> JSONObject.enumOr(key: String, def: E): E =
            runCatching { enumValueOf<E>(getString(key)) }.getOrDefault(def)

        fun fromJson(o: JSONObject): AppSettings {
            val d = AppSettings()
            val pk = o.optJSONArray("splitPackages")
            val pkgs = buildSet { if (pk != null) for (i in 0 until pk.length()) add(pk.getString(i)) }
            return AppSettings(
                consentVersion = o.optInt("consentVersion", 0),
                consentTime = o.optLong("consentTime", 0L),
                mode = o.enumOr("mode", d.mode),
                warpTunnel = o.enumOr("warpTunnel", d.warpTunnel),
                noise = o.optBoolean("noise", d.noise),
                scanner = o.optBoolean("scanner", d.scanner),
                searchNoise = o.optBoolean("searchNoise", d.searchNoise),
                dns = o.enumOr("dns", d.dns),
                ipv6 = o.enumOr("ipv6", d.ipv6),
                fragSize = o.optString("fragSize", d.fragSize),
                fragSleep = o.optString("fragSleep", d.fragSleep),
                fragmentStealth = o.optBoolean("fragmentStealth", d.fragmentStealth),
                splitMode = o.enumOr("splitMode", d.splitMode),
                splitPackages = pkgs,
                activeProfileId = if (o.isNull("activeProfileId")) null else o.optString("activeProfileId"),
                mtu = o.optInt("mtu", d.mtu)
            )
        }
    }
}

object SettingsStore {
    private lateinit var prefs: SharedPreferences
    private val _flow = MutableStateFlow(AppSettings())
    val flow: StateFlow<AppSettings> = _flow.asStateFlow()
    val value: AppSettings get() = _flow.value

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("silentgate_settings", Context.MODE_PRIVATE)
        val raw = prefs.getString("settings", null)
        if (raw != null) runCatching { _flow.value = AppSettings.fromJson(JSONObject(raw)) }
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_flow.value)
        _flow.value = next
        prefs.edit().putString("settings", next.toJson().toString()).apply()
    }
}

/** A user supplied Stealth profile: a vless:// link or a raw sing-box outbound/endpoint JSON object. */
data class Profile(val id: String, val name: String, val kind: String, val data: String) {
    val subtitle: String get() = if (kind == "vless") "VLESS link" else "Custom JSON"
}

object ProfileStore {
    private lateinit var prefs: SharedPreferences
    private val _flow = MutableStateFlow<List<Profile>>(emptyList())
    val flow: StateFlow<List<Profile>> = _flow.asStateFlow()

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("silentgate_profiles", Context.MODE_PRIVATE)
        val raw = prefs.getString("profiles", null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            _flow.value = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Profile(o.getString("id"), o.getString("name"), o.getString("kind"), o.getString("data"))
            }
        }
    }

    private fun save() {
        val arr = JSONArray()
        _flow.value.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind).put("data", it.data))
        }
        prefs.edit().putString("profiles", arr.toString()).apply()
    }

    /** Returns an error message or null when the profile was added. */
    fun import(text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return "Nothing to import"
        val profile: Profile = when {
            t.startsWith("vless://", ignoreCase = true) -> {
                val parsed = runCatching { VlessLink.parse(t) }.getOrElse { return "Invalid VLESS link: ${it.message}" }
                Profile(UUID.randomUUID().toString(), parsed.name, "vless", t)
            }
            t.startsWith("{") -> {
                val o = runCatching { JSONObject(t) }.getOrElse { return "Invalid JSON: ${it.message}" }
                val type = o.optString("type")
                if (type.isEmpty()) return "JSON needs a \"type\" field (vless, wireguard, awg ...)"
                Profile(UUID.randomUUID().toString(), o.optString("tag").ifEmpty { "Custom $type" }, "json", t)
            }
            else -> return "Paste a vless:// link or a sing-box outbound/endpoint JSON object"
        }
        _flow.value = _flow.value + profile
        save()
        if (SettingsStore.value.activeProfileId == null) SettingsStore.update { it.copy(activeProfileId = profile.id) }
        return null
    }

    fun remove(id: String) {
        _flow.value = _flow.value.filterNot { it.id == id }
        save()
        if (SettingsStore.value.activeProfileId == id) {
            SettingsStore.update { it.copy(activeProfileId = _flow.value.firstOrNull()?.id) }
        }
    }

    fun active(): Profile? {
        val id = SettingsStore.value.activeProfileId
        return _flow.value.firstOrNull { it.id == id } ?: _flow.value.firstOrNull()
    }
}

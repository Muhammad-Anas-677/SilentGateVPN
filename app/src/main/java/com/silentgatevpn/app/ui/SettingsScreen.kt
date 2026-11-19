package com.silentgatevpn.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.silentgatevpn.app.legal.LegalDoc
import com.silentgatevpn.app.legal.LegalDocs
import com.silentgatevpn.app.vpn.DnsProvider
import com.silentgatevpn.app.vpn.Ipv6Mode
import com.silentgatevpn.app.vpn.SettingsStore
import com.silentgatevpn.app.vpn.WarpTunnel

private data class FragPreset(val label: String, val size: String, val sleep: String)

@Composable
fun SettingsScreen(onOpenDoc: (LegalDoc) -> Unit) {
    val ctx = LocalContext.current
    val s by SettingsStore.flow.collectAsState()
    val presets = listOf(
        FragPreset("Light", "20-60", "1-4"),
        FragPreset("Balanced", "10-30", "2-8"),
        FragPreset("Strong", "1-10", "5-15")
    )
    val currentPreset = presets.firstOrNull { it.size == s.fragSize && it.sleep == s.fragSleep } ?: presets[1]

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
        Text("Changes apply the next time you connect.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        SectionTitle("WARP layer")
        SgCardBox {
            Text("Tunnel type", style = MaterialTheme.typography.titleMedium)
            Text("Auto races WireGuard, AmneziaWG and MASQUE and keeps the fastest one that answers.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            ChoiceChips(WarpTunnel.entries, s.warpTunnel, { it.label }) { t -> SettingsStore.update { it.copy(warpTunnel = t) } }
            Spacer(Modifier.height(12.dp))
            ToggleRow("Obfuscation noise", "Sends fake QUIC-looking packets before each WireGuard handshake.", s.noise) { v -> SettingsStore.update { it.copy(noise = v) } }
            ToggleRow("Endpoint scanner", "Finds a Cloudflare address and port that work on your current network (ISP or Wi-Fi).", s.scanner) { v -> SettingsStore.update { it.copy(scanner = v) } }
            ToggleRow("Search for noise preset", "If handshakes fail, tries several junk-packet presets and remembers the one that works.", s.searchNoise) { v -> SettingsStore.update { it.copy(searchNoise = v) } }
        }

        SectionTitle("Light layer (TLS fragmentation)")
        SgCardBox {
            Text("Splits the TLS hello into small pieces to defeat SNI-only filters. Does not hide your IP from the destination.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp))
            ChoiceChips(presets, currentPreset, { it.label }) { p -> SettingsStore.update { it.copy(fragSize = p.size, fragSleep = p.sleep) } }
            Spacer(Modifier.height(8.dp))
            ToggleRow("Fragment Stealth profiles", "Also fragment the TLS hello of your VLESS profile.", s.fragmentStealth) { v -> SettingsStore.update { it.copy(fragmentStealth = v) } }
        }

        SectionTitle("DNS")
        SgCardBox {
            Text("All DNS goes inside the tunnel over encrypted DoH. Family and SafeSearch resolvers are never used.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp))
            ChoiceChips(DnsProvider.entries, s.dns, { it.label }) { d -> SettingsStore.update { it.copy(dns = d) } }
        }

        SectionTitle("IPv6")
        SgCardBox {
            Text("IPv6 always enters the tunnel so your real IPv6 address cannot leak. Block rejects it; Tunnel forwards it.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp))
            ChoiceChips(Ipv6Mode.entries, s.ipv6, { it.label }) { m -> SettingsStore.update { it.copy(ipv6 = m) } }
        }

        SectionTitle("Kill switch")
        SgCardBox {
            Text("Android decides what happens when a VPN drops. The reliable switch is the system one: turn on Always-on VPN and Block connections without VPN for SilentGate.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Open Android VPN settings") }
        }

        SectionTitle("Legal")
        SgCardBox {
            LegalDocs.all.forEach { doc -> DocLinkRow(doc) { onOpenDoc(doc) } }
        }

        SectionTitle("About")
        SgCardBox {
            InfoLine("Version", "1.0.0")
            InfoLine("Package", "com.silentgatevpn.app")
            InfoLine("Core", "Hiddify sing-box (libbox)")
            InfoLine("Licence", "GPL-3.0, source public")
        }
        MadeByAnas()
    }
}

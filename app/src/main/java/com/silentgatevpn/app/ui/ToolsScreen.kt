package com.silentgatevpn.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.silentgatevpn.app.vpn.ExitInfo
import com.silentgatevpn.app.vpn.Net
import com.silentgatevpn.app.vpn.Status
import com.silentgatevpn.app.vpn.VpnState
import kotlinx.coroutines.launch

private data class Check(val title: String, val ok: Boolean?, val detail: String)

@Composable
fun ToolsScreen() {
    val scope = rememberCoroutineScope()
    val status by VpnState.status.collectAsState()
    val clipboard = LocalClipboardManager.current
    var running by remember { mutableStateOf(false) }
    var checks by remember { mutableStateOf<List<Check>>(emptyList()) }
    var exit by remember { mutableStateOf<ExitInfo?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var found by remember { mutableStateOf<List<Net.Candidate>>(emptyList()) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Tools", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Geo-verification and leak checker. Connect first so the tests describe your tunnel.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        SectionTitle("Leak and exit check")
        Button(
            onClick = {
                running = true
                scope.launch {
                    val out = mutableListOf<Check>()
                    checks = emptyList(); exit = null
                    val e = runCatching { Net.exitInfo() }
                    e.onSuccess {
                        exit = it
                        out += Check(
                            "Exit region", !it.restricted,
                            if (it.restricted) "${it.region.ifEmpty { it.country }} is on the age-verification watch list"
                            else "${listOf(it.city, it.region, it.country).filter { s -> s.isNotEmpty() }.joinToString(", ")} is not on the watch list"
                        )
                        out += Check("Latency", it.pingMs in 0..600, if (it.pingMs >= 0) "${it.pingMs} ms to Cloudflare" else "No reply")
                    }.onFailure { out += Check("Exit region", false, "Could not reach the check services: ${it.message}") }
                    checks = out.toList()

                    runCatching { Net.dnsLeak() }.onSuccess {
                        out += Check("DNS leak", !it.leaking, "Resolver ${it.resolverIp} ${it.resolverGeo}")
                    }.onFailure { out += Check("DNS leak", null, "Test unavailable: ${it.message}") }
                    checks = out.toList()

                    val v6 = Net.ipv6Probe()
                    out += if (v6 == null) Check("IPv6 leak", true, "No IPv6 address is exposed")
                    else Check("IPv6 leak", false, "IPv6 reachable as $v6. Set IPv6 to Block in Settings if this is your real address")
                    checks = out.toList()
                    running = false
                }
            },
            enabled = !running, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SgGreen, contentColor = Color.Black)
        ) {
            if (running) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
            else Text("Run checks")
        }
        if (status != Status.CONNECTED) {
            Text("You are not connected, so results describe your normal connection.", style = MaterialTheme.typography.bodySmall,
                color = SgOrange, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        checks.forEach { c ->
            SgCardBox(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (c.ok == false) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = when (c.ok) { true -> SgGreen; false -> SgDanger; null -> SgOrange }
                    )
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text(c.title, style = MaterialTheme.typography.titleMedium)
                        Text(c.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        exit?.let { e ->
            SgCardBox {
                InfoLine("Public IP", e.ip)
                InfoLine("Cloudflare edge", e.colo.ifEmpty { "-" })
                InfoLine("WARP", when (e.warpOn) { true -> "On"; false -> "Off"; null -> "-" })
            }
        }

        SectionTitle("Cloudflare address scanner")
        SgCardBox {
            Text(
                "Tests which Cloudflare edge addresses answer on port 443 from this network. Useful for picking a clean address for a Worker profile. WARP endpoints are scanned automatically by the core when the scanner is on in Settings. Run this while disconnected for the most useful result.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    scanning = true; found = emptyList(); progress = 0f
                    scope.launch {
                        found = Net.scanCloudflare { d, t -> progress = d.toFloat() / t }
                        scanning = false
                    }
                },
                enabled = !scanning
            ) { Text(if (scanning) "Scanning…" else "Scan") }
            if (scanning) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), color = SgGreen)
            }
            found.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.ip, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${c.ms} ms", color = SgGreenSoft, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(c.ip)) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp)) }
                }
            }
            if (!scanning && found.isEmpty() && progress > 0f) {
                Text("No address answered.", color = SgOrange, modifier = Modifier.padding(top = 8.dp))
            }
        }

        SectionTitle("Event log")
        val log by VpnState.log.collectAsState()
        SgCardBox {
            if (log.isEmpty()) Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            log.takeLast(25).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        MadeByAnas()
    }
}

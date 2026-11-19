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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.silentgatevpn.app.vpn.Profile
import com.silentgatevpn.app.vpn.ProfileStore
import com.silentgatevpn.app.vpn.SettingsStore

@Composable
fun ProfilesScreen() {
    val profiles by ProfileStore.flow.collectAsState()
    val settings by SettingsStore.flow.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Profiles", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
        Text(
            "WARP needs no setup. Stealth profiles are your own VLESS or WireGuard/AmneziaWG servers, used as the backup layer or for switching exit countries.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        SectionTitle("Built in")
        SgCardBox {
            Text("Cloudflare WARP", style = MaterialTheme.typography.titleMedium)
            Text(
                "Automatic free account, endpoint scanner and noise handled by the core. Tunnel type is chosen in Settings.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SectionTitle("Stealth profiles")
        if (profiles.isEmpty()) {
            SgCardBox { Text("No profiles yet. Add a vless:// link or a custom JSON profile.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        val activeId = ProfileStore.active()?.id
        profiles.forEach { p: Profile ->
            SgCardBox(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = p.id == activeId,
                        onClick = { SettingsStore.update { it.copy(activeProfileId = p.id) } },
                        colors = RadioButtonDefaults.colors(selectedColor = SgGreen, unselectedColor = Color(0xFF8A8A8A))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(p.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { ProfileStore.remove(p.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = SgDanger)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SgGreen, contentColor = Color.Black)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Add profile")
        }

        SectionTitle("Free Cloudflare Worker (optional)")
        SgCardBox {
            Text(
                "Backup layer that looks like normal HTTPS to a Cloudflare domain. Free plan: 100,000 requests per day.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (guide) {
                Spacer(Modifier.height(8.dp))
                listOf(
                    "1. Sign in to dash.cloudflare.com (no card needed) and create a Worker.",
                    "2. Copy the script below, paste it into the Worker and set your own UUID.",
                    "3. Deploy. Your address will look like name.account.workers.dev.",
                    "4. Add a profile with this link (edit the three placeholders):",
                    "vless://YOUR-UUID@name.account.workers.dev:443?encryption=none&security=tls&sni=name.account.workers.dev&type=ws&host=name.account.workers.dev&path=%2F#My-Worker",
                    "Note: workers.dev names can be blocked by some networks, and Cloudflare's terms may limit proxy use. Use at your own risk."
                ).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(WORKER_SCRIPT.trim())) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Copy Worker script")
                }
            }
            TextButton(onClick = { guide = !guide }) { Text(if (guide) "Hide guide" else "Show setup guide", color = SgGreen) }
        }
        MadeByAnas()
    }

    if (showAdd) {
        var text by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = Color(0xFF121212),
            title = { Text("Add profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it; error = null }, minLines = 4, maxLines = 8,
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("vless://... or {\"type\":\"vless\", ...}") },
                        isError = error != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SgGreen, unfocusedBorderColor = Color(0xFF3A3A3A),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SgGreen
                        )
                    )
                    error?.let { Text(it, color = SgDanger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                    TextButton(onClick = { clipboard.getText()?.text?.let { text = it } }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp), tint = SgGreen)
                        Spacer(Modifier.size(6.dp))
                        Text("Paste from clipboard", color = SgGreen)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val err = ProfileStore.import(text)
                    if (err == null) showAdd = false else error = err
                }) { Text("Add", color = SgGreen) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel", color = Color.White) } }
        )
    }
}

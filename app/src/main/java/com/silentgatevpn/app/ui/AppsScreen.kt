package com.silentgatevpn.app.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.silentgatevpn.app.vpn.SettingsStore
import com.silentgatevpn.app.vpn.SplitMode
import com.silentgatevpn.app.vpn.Status
import com.silentgatevpn.app.vpn.VpnState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppItem(val pkg: String, val label: String)

private val LOCAL_KEYWORDS = listOf(
    "jazzcash", "easypaisa", "sadapay", "nayapay", "hbl", "meezan", "ubl", "alfalah", "askari", "bank",
    "foodpanda", "bykea", "careem", "daraz", "keenu", "yango", "indrive"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen() {
    val ctx = LocalContext.current
    val settings by SettingsStore.flow.collectAsState()
    val status by VpnState.status.collectAsState()
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .filter { it.pkg != ctx.packageName }
                .distinctBy { it.pkg }
                .sortedBy { it.label.lowercase() }
        }
    }

    val shown = apps.filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }
    val modes = SplitMode.entries
    val short = mapOf(SplitMode.OFF to "Off", SplitMode.EXCLUDE to "Bypass", SplitMode.ONLY to "Only")

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Text("Split tunnelling", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Bypass: selected apps skip the VPN (good for banking and local apps). Only: just the selected apps use the VPN.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = settings.splitMode == m,
                    onClick = { SettingsStore.update { it.copy(splitMode = m) } },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = SgGreen, activeContentColor = Color.Black,
                        inactiveContainerColor = Color(0xFF101010), inactiveContentColor = Color.White
                    )
                ) { Text(short.getValue(m)) }
            }
        }
        if (status == Status.CONNECTED || status == Status.CONNECTING) {
            Text("Changes apply the next time you connect.", style = MaterialTheme.typography.bodySmall,
                color = SgOrange, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SgGreen, unfocusedBorderColor = Color(0xFF3A3A3A),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SgGreen
            )
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val picks = apps.filter { a -> LOCAL_KEYWORDS.any { a.label.contains(it, true) } }.map { it.pkg }.toSet()
                SettingsStore.update { it.copy(splitPackages = it.splitPackages + picks, splitMode = if (it.splitMode == SplitMode.OFF) SplitMode.EXCLUDE else it.splitMode) }
            }) { Text("Pick banking & local") }
            OutlinedButton(onClick = { SettingsStore.update { it.copy(splitPackages = emptySet()) } }) { Text("Clear (${settings.splitPackages.size})") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.pkg }) { app ->
                val checked = app.pkg in settings.splitPackages
                val icon by produceIcon(app.pkg)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    icon?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp)) }
                        ?: Spacer(Modifier.size(40.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(app.pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { on ->
                            SettingsStore.update { s ->
                                s.copy(splitPackages = if (on) s.splitPackages + app.pkg else s.splitPackages - app.pkg)
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = SgGreen, checkmarkColor = Color.Black, uncheckedColor = Color(0xFF8A8A8A))
                    )
                }
            }
        }
    }
}

@Composable
private fun produceIcon(pkg: String): androidx.compose.runtime.State<Bitmap?> {
    val ctx = LocalContext.current
    return androidx.compose.runtime.produceState<Bitmap?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96) }.getOrNull()
        }
    }
}

package com.silentgatevpn.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.silentgatevpn.app.legal.LegalDoc
import com.silentgatevpn.app.vpn.SettingsStore

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    APPS("Apps", Icons.Rounded.Apps),
    PROFILES("Profiles", Icons.Rounded.VpnKey),
    TOOLS("Tools", Icons.Rounded.NetworkCheck),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun SilentGateRoot(onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val settings by SettingsStore.flow.collectAsState()
    var legal by remember { mutableStateOf<LegalDoc?>(null) }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME.name) }

    val doc = legal
    if (doc != null) {
        BackHandler { legal = null }
        LegalViewer(doc = doc, onBack = { legal = null })
        return
    }

    if (!settings.consented) {
        ConsentScreen(
            onOpenDoc = { legal = it },
            onAccept = {
                SettingsStore.update { it.copy(consentVersion = com.silentgatevpn.app.vpn.CONSENT_VERSION, consentTime = System.currentTimeMillis()) }
            }
        )
        return
    }

    val current = Tab.valueOf(tab)
    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0A0A), contentColor = Color.White) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == current,
                        onClick = { tab = t.name },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = SgGreen,
                            indicatorColor = SgGreen,
                            unselectedIconColor = Color(0xFFB0B0B0),
                            unselectedTextColor = Color(0xFFB0B0B0)
                        )
                    )
                }
            }
        }
    ) { pad: PaddingValues ->
        Box(Modifier.padding(pad)) {
            when (current) {
                Tab.HOME -> HomeScreen(onConnect, onDisconnect)
                Tab.APPS -> AppsScreen()
                Tab.PROFILES -> ProfilesScreen()
                Tab.TOOLS -> ToolsScreen()
                Tab.SETTINGS -> SettingsScreen(onOpenDoc = { legal = it })
            }
        }
    }
}

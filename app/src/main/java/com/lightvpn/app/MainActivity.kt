package com.lightvpn.app

import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lightvpn.app.ui.AppTheme
import com.lightvpn.app.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: VpnViewModel = viewModel()
            val amoled by vm.amoled.collectAsStateWithLifecycle()
            val ui by vm.ui.collectAsStateWithLifecycle()

            // Status/navigation bar icons follow the active theme.
            SideEffect {
                val style = if (amoled) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            val permission = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) vm.connect() else vm.onPermissionDenied()
            }

            AppTheme(amoled = amoled) {
                HomeScreen(
                    ui = ui,
                    amoled = amoled,
                    onThemeChange = vm::setAmoled,
                    onPower = {
                        if (ui.status == Status.Connected) {
                            vm.disconnect()
                        } else {
                            val intent = VpnService.prepare(this@MainActivity)
                            if (intent != null) permission.launch(intent) else vm.connect()
                        }
                    }
                )
            }
        }
    }
}

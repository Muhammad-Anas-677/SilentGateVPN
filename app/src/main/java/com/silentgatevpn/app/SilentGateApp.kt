package com.silentgatevpn.app

import android.app.Application
import android.os.Build
import com.silentgatevpn.app.vpn.ProfileStore
import com.silentgatevpn.app.vpn.SettingsStore
import com.silentgatevpn.app.vpn.VpnState
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import java.io.File

class SilentGateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        ProfileStore.init(this)
        runCatching {
            val work = File(filesDir, "work").apply { mkdirs() }
            val opts = SetupOptions()
            opts.setBasePath(filesDir.path)
            opts.setWorkingPath(work.path)
            opts.setTempPath(cacheDir.path)
            opts.setFixAndroidStack(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            opts.setLogMaxLines(500L)
            opts.setDebug(false)
            Libbox.setup(opts)
        }.onFailure { VpnState.log("Core setup failed: ${it.message}") }
    }
}

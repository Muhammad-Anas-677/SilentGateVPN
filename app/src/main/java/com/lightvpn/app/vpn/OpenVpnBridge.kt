package com.lightvpn.app.vpn

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Hands a VPN Gate profile to the free, open-source "OpenVPN for Android" app (de.blinkt.openvpn). */
object OpenVpnBridge {

    private const val PKG = "de.blinkt.openvpn"
    private const val EXTRA_PROFILE = "de.blinkt.openvpn.api.profileName"

    // Profile name OpenVPN for Android should show (taken from the imported file name).
    fun profileName(code: String) = "LightVPN-$code"

    fun isInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(PKG, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Opens OpenVPN for Android's import screen with the profile (user taps the save check mark). */
    fun importIntent(ctx: Context, code: String, config: String): Intent {
        val dir = File(ctx.cacheDir, "ovpn").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, profileName(code) + ".ovpn")
        file.writeText(config, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/x-openvpn-profile")
            .setPackage(PKG)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun startIntent(code: String): Intent =
        Intent().setClassName(PKG, "de.blinkt.openvpn.api.ConnectVPN")
            .putExtra(EXTRA_PROFILE, profileName(code))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun stopIntent(): Intent =
        Intent().setClassName(PKG, "de.blinkt.openvpn.api.DisconnectVPN")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun storeIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PKG"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

package com.lightvpn.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores the WireGuard config (contains the private key) encrypted with Android Keystore. */
class SecureStore(private val ctx: Context) {

    private fun open() = EncryptedSharedPreferences.create(
        ctx,
        FILE,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val prefs by lazy {
        try {
            open()
        } catch (e: Exception) {
            // Keystore/prefs corrupted on some devices: wipe and recreate (a new config is registered).
            ctx.deleteSharedPreferences(FILE)
            open()
        }
    }

    fun load(): String? = prefs.getString(KEY, null)

    fun save(config: String) {
        prefs.edit().putString(KEY, config).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val FILE = "vpn_secure"
        const val KEY = "wg_config"
    }
}

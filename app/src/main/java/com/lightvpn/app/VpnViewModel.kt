package com.lightvpn.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lightvpn.app.data.SecureStore
import com.lightvpn.app.vpn.WarpRegistrar
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

private val Context.settingsStore by preferencesDataStore(name = "settings")
private val AMOLED_KEY = booleanPreferencesKey("amoled")

enum class Status { Disconnected, Setting, Connecting, Connected }

data class UiState(val status: Status = Status.Disconnected, val error: String? = null)

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val store by lazy { SecureStore(ctx) }
    private val backend by lazy { GoBackend(ctx) }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** false = Light (default), true = AMOLED dark. Saved with DataStore. */
    val amoled: StateFlow<Boolean> = ctx.settingsStore.data
        .map { it[AMOLED_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val tunnel = object : Tunnel {
        override fun getName(): String = "warp"
        override fun onStateChange(newState: Tunnel.State) {
            _ui.value = if (newState == Tunnel.State.UP) UiState(Status.Connected) else UiState()
        }
    }

    fun setAmoled(value: Boolean) {
        viewModelScope.launch { ctx.settingsStore.edit { it[AMOLED_KEY] = value } }
    }

    fun connect() {
        if (_ui.value.status != Status.Disconnected) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _ui.value = UiState(Status.Setting)
                var text = store.load()
                if (text == null) {
                    text = WarpRegistrar.register()
                    store.save(text)
                }
                _ui.value = UiState(Status.Connecting)
                val cfg = Config.parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
                backend.setState(tunnel, Tunnel.State.UP, cfg)
                _ui.value = UiState(Status.Connected)
            } catch (e: Exception) {
                if (e is BadConfigException) store.clear()
                _ui.value = UiState(error = friendly(e))
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
            _ui.value = UiState()
        }
    }

    fun onPermissionDenied() {
        _ui.value = UiState(error = "VPN permission is required to connect.")
    }

    private fun friendly(e: Exception): String = when (e) {
        is WarpRegistrar.WarpException -> e.message ?: "Setup failed."
        is java.io.IOException -> "Network error. Check your internet and try again."
        else -> "Couldn't start the VPN. Please try again."
    }
}

package com.silentgatevpn.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow

enum class Status { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

data class ExitInfo(
    val ip: String,
    val country: String,
    val countryCode: String,
    val region: String,
    val regionCode: String,
    val city: String,
    val warpOn: Boolean?,
    val colo: String,
    val pingMs: Long,
    val restricted: Boolean
)

object VpnState {
    val status = MutableStateFlow(Status.DISCONNECTED)
    val layer = MutableStateFlow<ConfigBuilder.Layer?>(null)
    val message = MutableStateFlow("")
    val exit = MutableStateFlow<ExitInfo?>(null)
    val rxSpeed = MutableStateFlow(0L)
    val txSpeed = MutableStateFlow(0L)
    val sessionRx = MutableStateFlow(0L)
    val sessionTx = MutableStateFlow(0L)
    val alwaysOn = MutableStateFlow(false)
    val lockdown = MutableStateFlow(false)
    val log = MutableStateFlow<List<String>>(emptyList())
    val lastFailure = MutableStateFlow<List<String>>(emptyList())

    fun log(line: String) {
        log.value = (log.value + line).takeLast(80)
    }

    fun resetSession() {
        rxSpeed.value = 0; txSpeed.value = 0; sessionRx.value = 0; sessionTx.value = 0
        exit.value = null
    }
}

fun formatBytes(b: Long): String {
    if (b < 1024) return "$b B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = b.toDouble() / 1024
    var i = 0
    while (v >= 1024 && i < units.size - 1) { v /= 1024; i++ }
    return String.format(java.util.Locale.US, "%.1f %s", v, units[i])
}

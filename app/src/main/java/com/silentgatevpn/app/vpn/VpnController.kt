package com.silentgatevpn.app.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object VpnController {
    fun start(ctx: Context) {
        ContextCompat.startForegroundService(
            ctx, Intent(ctx, SilentGateService::class.java).setAction(SilentGateService.ACTION_START)
        )
    }

    fun stop(ctx: Context) {
        ctx.startService(Intent(ctx, SilentGateService::class.java).setAction(SilentGateService.ACTION_STOP))
    }
}

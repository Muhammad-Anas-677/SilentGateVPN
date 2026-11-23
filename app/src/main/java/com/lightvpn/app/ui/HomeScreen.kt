package com.lightvpn.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lightvpn.app.Status
import com.lightvpn.app.UiState

@Composable
fun HomeScreen(
    ui: UiState,
    amoled: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onPower: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val busy = ui.status == Status.Setting || ui.status == Status.Connecting
    val on = ui.status == Status.Connected

    val buttonColor = when {
        on -> colors.primary
        busy -> colors.primaryContainer
        else -> colors.surfaceVariant
    }
    val iconColor = when {
        on -> colors.onPrimary
        busy -> colors.onPrimaryContainer
        else -> colors.onSurfaceVariant
    }
    val title = when (ui.status) {
        Status.Disconnected -> "Not connected"
        Status.Setting -> "Setting up…"
        Status.Connecting -> "Connecting…"
        Status.Connected -> "Connected"
    }
    val hint = when (ui.status) {
        Status.Disconnected -> "Tap the button to connect"
        Status.Connected -> "Tap the button to disconnect"
        else -> "Please wait"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Top bar: theme toggle (off = Light, on = AMOLED dark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Light", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                Switch(
                    checked = amoled,
                    onCheckedChange = onThemeChange,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text("AMOLED", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(212.dp),
                            color = colors.primary,
                            strokeWidth = 4.dp
                        )
                    }
                    Surface(
                        onClick = onPower,
                        enabled = !busy,
                        shape = CircleShape,
                        color = buttonColor,
                        contentColor = iconColor,
                        border = if (on) null else BorderStroke(2.dp, colors.outline),
                        modifier = Modifier
                            .size(180.dp)
                            .semantics { contentDescription = if (on) "Disconnect" else "Connect" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PowerIcon(color = iconColor, modifier = Modifier.size(72.dp))
                        }
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (ui.error != null) {
                    Text(
                        text = ui.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.1f, cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(s * 0.15f, s * 0.2f),
            size = Size(s * 0.7f, s * 0.7f),
            style = stroke
        )
        drawLine(
            color = color,
            start = Offset(s * 0.5f, s * 0.08f),
            end = Offset(s * 0.5f, s * 0.5f),
            strokeWidth = s * 0.1f,
            cap = StrokeCap.Round
        )
    }
}

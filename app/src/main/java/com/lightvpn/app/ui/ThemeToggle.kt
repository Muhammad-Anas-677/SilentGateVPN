package com.lightvpn.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Theme toggle made of ONE icon and no text.
 * Light theme shows a moon (tap = switch to dark/AMOLED); dark theme shows a sun (tap = switch to light).
 * The icon colour is onBackground, so it stays readable in both themes. Touch target is 48dp.
 * Reusable: copy this file + call ThemeToggleButton(dark, onToggle) from any screen.
 */
@Composable
fun ThemeToggleButton(
    dark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.onBackground
    IconButton(
        onClick = onToggle,
        modifier = modifier.semantics {
            // Read by TalkBack only; nothing is drawn as text.
            contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme"
        }
    ) {
        Crossfade(targetState = dark, label = "theme-icon") { isDark ->
            if (isDark) SunIcon(tint, Modifier.size(24.dp)) else MoonIcon(tint, Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SunIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(s / 2f, s / 2f)
        drawCircle(color = color, radius = s * 0.2f, center = c)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(
                color = color,
                start = Offset(c.x + dx * s * 0.31f, c.y + dy * s * 0.31f),
                end = Offset(c.x + dx * s * 0.43f, c.y + dy * s * 0.43f),
                strokeWidth = s * 0.085f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun MoonIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        // Crescent = full circle minus a second circle shifted to the upper right.
        val full = Path().apply {
            addOval(Rect(center = Offset(s * 0.48f, s * 0.52f), radius = s * 0.40f))
        }
        val bite = Path().apply {
            addOval(Rect(center = Offset(s * 0.68f, s * 0.36f), radius = s * 0.33f))
        }
        drawPath(Path.combine(PathOperation.Difference, full, bite), color)
    }
}

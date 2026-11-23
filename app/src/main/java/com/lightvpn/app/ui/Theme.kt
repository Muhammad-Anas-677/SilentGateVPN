package com.lightvpn.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF565F71),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

/** AMOLED: true black background, high-contrast text, subtle grey containers + outlines. */
private val AmoledScheme = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF0B2A52),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF1A1B1F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun AppTheme(amoled: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (amoled) AmoledScheme else LightScheme, content = content)
}

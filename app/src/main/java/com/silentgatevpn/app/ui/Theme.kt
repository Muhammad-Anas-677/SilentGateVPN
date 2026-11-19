package com.silentgatevpn.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SgGreen = Color(0xFF00E676)
val SgGreenSoft = Color(0xFF69F0AE)
val SgOrange = Color(0xFFFF9800)
val SgDanger = Color(0xFFFF5252)
val SgWarnBg = Color(0xFF2B1A00)
val SgDangerBg = Color(0xFF2E0A0A)
val SgCard = Color(0xFF101010)
val SgCardHigh = Color(0xFF171717)
val SgTextDim = Color(0xFFA8A8A8)

private val AmoledScheme = darkColorScheme(
    primary = SgGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00381B),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = SgGreenSoft,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0F2A1B),
    onSecondaryContainer = Color(0xFFCFF5DD),
    tertiary = SgOrange,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color(0xFFF2F2F2),
    surface = Color.Black,
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = SgTextDim,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF171717),
    surfaceContainerHighest = Color(0xFF1F1F1F),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF2A2A2A),
    error = SgDanger,
    onError = Color.Black,
    errorContainer = SgDangerBg,
    onErrorContainer = Color(0xFFFFB4AB)
)

private val SgTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun SilentGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AmoledScheme, typography = SgTypography) {
        // Surface supplies LocalContentColor (onBackground). Without it, any Text outside a Scaffold
        // falls back to black and becomes invisible on the black background.
        Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}

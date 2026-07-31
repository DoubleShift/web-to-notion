package io.trae.webtonotion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 魅族便签风格主题

private val LightColorScheme = lightColorScheme(
    primary = MemoYellow,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF4E0),
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = Color.White,
    background = BackgroundGrey,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundGrey,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEEEEEE),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = MemoYellowDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3D3118),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9A9A9A),
    onSecondary = Color.Black,
    background = Color(0xFF1C1C1E),
    onBackground = Color.White,
    surface = Color(0xFF2C2C2E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2C2C2E),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun WebToNotionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HarmonyTypography,
        shapes = HarmonyShapes,
        content = content
    )
}

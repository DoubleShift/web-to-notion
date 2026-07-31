package io.trae.webtonotion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 华为 HarmonyOS Design 色彩 → Material3 ColorScheme 映射
// 参照 HuaweiDesign.md §2.1

private val LightColorScheme = lightColorScheme(
    primary = BrandLight,
    onPrimary = BackgroundPrimaryLight,
    primaryContainer = InteractiveSelectLight,
    onPrimaryContainer = FontPrimaryLight,
    secondary = FontSecondaryLight,
    onSecondary = BackgroundPrimaryLight,
    tertiary = ConfirmColor,
    onTertiary = BackgroundPrimaryLight,
    error = WarningColor,
    onError = BackgroundPrimaryLight,
    background = BackgroundPrimaryLight,
    onBackground = FontPrimaryLight,
    surface = BackgroundPrimaryLight,
    onSurface = FontPrimaryLight,
    surfaceVariant = BackgroundSecondaryLight,
    onSurfaceVariant = FontSecondaryLight,
    surfaceContainer = BackgroundTertiaryLight,
    surfaceContainerHigh = BackgroundTertiaryLight,
    surfaceContainerHighest = BackgroundFourthLight,
    outline = FontTertiaryLight,
    outlineVariant = CompDividerLight,
    inverseSurface = BackgroundSecondaryDark,
    inverseOnSurface = FontPrimaryDark
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandDark,
    onPrimary = BackgroundPrimaryDark,
    primaryContainer = InteractiveSelectDark,
    onPrimaryContainer = FontPrimaryDark,
    secondary = FontSecondaryDark,
    onSecondary = BackgroundPrimaryDark,
    tertiary = ConfirmColor,
    onTertiary = BackgroundPrimaryDark,
    error = WarningColor,
    onError = BackgroundPrimaryDark,
    background = BackgroundSecondaryDark,
    onBackground = FontPrimaryDark,
    surface = BackgroundSecondaryDark,
    onSurface = FontPrimaryDark,
    surfaceVariant = BackgroundTertiaryDark,
    onSurfaceVariant = FontSecondaryDark,
    surfaceContainer = BackgroundTertiaryDark,
    surfaceContainerHigh = BackgroundFourthDark,
    surfaceContainerHighest = BackgroundFourthDark,
    outline = FontTertiaryDark,
    outlineVariant = CompDividerDark,
    inverseSurface = BackgroundSecondaryLight,
    inverseOnSurface = FontPrimaryLight
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

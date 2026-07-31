package io.trae.webtonotion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 华为 HarmonyOS Design 字体层级 (Phone)
// 参照 HuaweiDesign.md §2.2
// HarmonyOS Sans 在 Android 上 fallback 到系统 Sans Serif

private val DefaultFontFamily = FontFamily.SansSerif

val HarmonyTypography = Typography(
    // Display (Light 字重, 大标题展示)
    displayLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 38.sp,
        lineHeight = 52.sp
    ),
    displayMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 64.sp
    ),

    // Title (Bold 字重, 页面标题)
    titleLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,        // Title_L
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,        // Title_S
        lineHeight = 28.sp
    ),

    // Subtitle (Medium 字重, 副标题/卡片标题) — mapped to headlineSmall/titleSmall
    headlineSmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,        // Subtitle_L
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,        // Subtitle_M
        lineHeight = 22.sp
    ),

    // Body (Medium/Regular, 正文)
    bodyLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,        // Body_L
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,        // Body_M
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,        // Body_S
        lineHeight = 18.sp
    ),

    // Caption (Medium, 辅助说明)
    labelLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,        // Caption_L
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,        // Caption_M
        lineHeight = 14.sp
    )
)

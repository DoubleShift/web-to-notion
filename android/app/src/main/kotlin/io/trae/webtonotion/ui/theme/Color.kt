package io.trae.webtonotion.ui.theme

import androidx.compose.ui.graphics.Color

// 华为 HarmonyOS Design 色彩 Token (Light)
// 参照 HuaweiDesign.md §2.1
// 华为色值前两位为 Alpha 通道，如 E5000000 = 90% 不透明度黑

// 品牌色
val BrandLight = Color(0xFF0A59F7)
val BrandDark = Color(0xFF317AF7)

// 语义色
val ConfirmColor = Color(0xFF64BB5C)   // 确认/成功
val WarningColor = Color(0xFFE84026)   // 一级警示
val AlertColor = Color(0xFFED6F21)     // 二级警示

// Light 文本色
val FontPrimaryLight = Color(0xE6000000)   // α=90%
val FontSecondaryLight = Color(0x99000000) // α=60%
val FontTertiaryLight = Color(0x66000000)  // α=40%
val FontFourthLight = Color(0x33000000)    // α=20%
val FontEmphasizeLight = BrandLight

// Dark 文本色
val FontPrimaryDark = Color(0xFFFFFFFF)
val FontSecondaryDark = Color(0x99FFFFFF)
val FontTertiaryDark = Color(0x66FFFFFF)
val FontFourthDark = Color(0x33FFFFFF)
val FontEmphasizeDark = BrandDark

// Light 背景色
val BackgroundPrimaryLight = Color(0xFFFFFFFF)
val BackgroundSecondaryLight = Color(0xFFF1F3F5)
val BackgroundTertiaryLight = Color(0xFFE5E5EA)
val BackgroundFourthLight = Color(0xFFD1D1D6)
val BackgroundEmphasizeLight = BrandLight

// Dark 背景色
val BackgroundPrimaryDark = Color(0xFFE5E5E5)
val BackgroundSecondaryDark = Color(0xFF191A1C)
val BackgroundTertiaryDark = Color(0xFF202224)
val BackgroundFourthDark = Color(0xFF2E3033)
val BackgroundEmphasizeDark = BrandDark

// 交互色
val InteractiveHoverLight = Color(0x0C000000)   // α=5%
val InteractivePressedLight = Color(0x19000000)  // α=10%
val InteractiveFocusLight = BrandLight
val InteractiveActiveLight = BrandLight
val InteractiveSelectLight = Color(0x330A59F7)   // α=20% brand
val InteractiveClickLight = Color(0x19000000)

val InteractiveHoverDark = Color(0x0CFFFFFF)
val InteractivePressedDark = Color(0x19FFFFFF)
val InteractiveFocusDark = BrandDark
val InteractiveActiveDark = BrandDark
val InteractiveSelectDark = Color(0x33317AF7)
val InteractiveClickDark = Color(0x19FFFFFF)

// 分割线
val CompDividerLight = Color(0x33000000)  // α=20%
val CompDividerDark = Color(0x33FFFFFF)

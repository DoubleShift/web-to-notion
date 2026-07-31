package io.trae.webtonotion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 华为 HarmonyOS Design 圆角参数
// 参照 HuaweiDesign.md §2.5
// XS=4 / S=8 / M=12 / L=16 / XL=24

val HarmonyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // CornerRadius_XS
    small = RoundedCornerShape(8.dp),        // CornerRadius_S
    medium = RoundedCornerShape(12.dp),      // CornerRadius_M (卡片/按钮/输入框)
    large = RoundedCornerShape(16.dp),       // CornerRadius_L (弹窗)
    extraLarge = RoundedCornerShape(24.dp)   // CornerRadius_XL (封面)
)

package com.tktkcompany.shogi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

// ---- 和の配色 ----
/** 墨 */
val Sumi = Color(0xFF2B2622)
/** 生成り */
val Kinari = Color(0xFFF4ECDC)
/** 和紙の影 */
val WashiShade = Color(0xFFE6D9BF)
/** 朱 */
val Shu = Color(0xFFB8382A)
/** 金茶(縁取り) */
val Kincha = Color(0xFFB8893B)

// ---- 盤と駒 ----
val BoardLight = Color(0xFFF0C987)
val BoardDark = Color(0xFFDDAA62)
val BoardGrain = Color(0xFFB9813E)
val BoardEdge = Color(0xFF7A4B1E)
val BoardLine = Color(0xFF3A2614)
val StandLight = Color(0xFFD9A867)
val StandDark = Color(0xFFBF8A49)
val PieceLight = Color(0xFFFBE7B8)
val PieceDark = Color(0xFFE3BE7C)
val PieceEdge = Color(0xFF8A5A26)
val PieceText = Color(0xFF1C1612)
val PromotedText = Shu

// ---- ハイライト ----
val SelectedSquare = Color(0x6638A169)
val TargetDot = Color(0x99306E4A)
val LastMoveSquare = Color(0x55E8743B)

/** 明朝体(端末の Serif フォント。日本語環境では Noto Serif CJK など)。 */
val Mincho = FontFamily.Serif

private val ColorScheme = lightColorScheme(
    primary = Shu,
    onPrimary = Kinari,
    secondary = Sumi,
    onSecondary = Kinari,
    tertiary = Kincha,
    background = Kinari,
    onBackground = Sumi,
    surface = Kinari,
    onSurface = Sumi,
    surfaceVariant = WashiShade,
    outline = Kincha,
)

private fun TextStyle.mincho() = copy(fontFamily = Mincho)

private val ShogiTypography = Typography().run {
    copy(
        displayLarge = displayLarge.mincho(),
        displayMedium = displayMedium.mincho(),
        displaySmall = displaySmall.mincho(),
        headlineLarge = headlineLarge.mincho(),
        headlineMedium = headlineMedium.mincho(),
        headlineSmall = headlineSmall.mincho(),
        titleLarge = titleLarge.mincho(),
        titleMedium = titleMedium.mincho(),
        titleSmall = titleSmall.mincho(),
        bodyLarge = bodyLarge.mincho(),
        bodyMedium = bodyMedium.mincho(),
        bodySmall = bodySmall.mincho(),
        labelLarge = labelLarge.mincho(),
        labelMedium = labelMedium.mincho(),
        labelSmall = labelSmall.mincho(),
    )
}

@Composable
fun ShogiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = ShogiTypography, content = content)
}

package com.tktkcompany.shogi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BoardWood = Color(0xFFE8C27A)
val BoardLine = Color(0xFF3E2A14)
val StandWood = Color(0xFFC79A55)
val PieceText = Color(0xFF1A1A1A)
val PromotedText = Color(0xFFB71C1C)
val SelectedSquare = Color(0x8843A047)
val TargetSquare = Color(0x5542A5F5)
val LastMoveSquare = Color(0x44FF7043)

private val ColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C2F),
    secondary = Color(0xFF8D6E63),
    background = Color(0xFFF5EBDD),
    surface = Color(0xFFF5EBDD),
)

@Composable
fun ShogiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, content = content)
}

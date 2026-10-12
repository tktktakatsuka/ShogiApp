package com.tktkcompany.shogi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tktkcompany.shogi.ui.theme.Kincha
import com.tktkcompany.shogi.ui.theme.Kinari
import com.tktkcompany.shogi.ui.theme.Mincho
import com.tktkcompany.shogi.ui.theme.Shu
import com.tktkcompany.shogi.ui.theme.Sumi
import com.tktkcompany.shogi.ui.theme.WashiShade
import kotlin.random.Random

/** 繊維の筋 (位置・角度・長さは 0〜1 の比率)。 */
private class Fiber(val x: Float, val y: Float, val dx: Float, val dy: Float, val alpha: Float)

private val fibers: List<Fiber> = Random(11).let { r ->
    List(140) {
        val len = 0.02f + r.nextFloat() * 0.06f
        val angle = r.nextFloat() * 6.283f
        Fiber(
            x = r.nextFloat(),
            y = r.nextFloat(),
            dx = len * kotlin.math.cos(angle),
            dy = len * kotlin.math.sin(angle),
            alpha = 0.05f + r.nextFloat() * 0.08f,
        )
    }
}

/** 和紙風の背景(生成りのグラデーション + 細い繊維の筋)。 */
fun Modifier.washiBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(Kinari, WashiShade)))
    .drawBehind {
        val scale = maxOf(size.width, size.height)
        for (f in fibers) {
            val start = Offset(f.x * size.width, f.y * size.height)
            drawLine(
                color = Sumi.copy(alpha = f.alpha),
                start = start,
                end = start + Offset(f.dx * scale, f.dy * scale),
                strokeWidth = 1.2f,
            )
        }
    }

/** 朱色のメインボタン。 */
@Composable
fun ShuButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, Kincha),
        colors = ButtonDefaults.buttonColors(
            containerColor = Shu,
            contentColor = Kinari,
            disabledContainerColor = Sumi.copy(alpha = 0.15f),
            disabledContentColor = Sumi.copy(alpha = 0.4f),
        ),
        modifier = modifier.height(height),
    ) {
        Text(text, fontFamily = Mincho, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 1.sp)
    }
}

/** 墨色の枠線ボタン。 */
@Composable
fun SumiButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, if (enabled) Sumi else Sumi.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Sumi),
        modifier = modifier.height(48.dp),
    ) {
        Text(text, fontFamily = Mincho, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

package com.tktkcompany.shogi.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tktkcompany.shogi.game.PieceType
import com.tktkcompany.shogi.game.Player
import com.tktkcompany.shogi.ui.theme.BoardEdge
import com.tktkcompany.shogi.ui.theme.Kincha
import com.tktkcompany.shogi.ui.theme.Kinari
import com.tktkcompany.shogi.ui.theme.Mincho
import com.tktkcompany.shogi.ui.theme.Shu
import com.tktkcompany.shogi.ui.theme.StandDark
import com.tktkcompany.shogi.ui.theme.StandLight
import com.tktkcompany.shogi.ui.theme.Sumi

/**
 * 駒台。プレイヤー名・手番マーク・持ち駒(枚数バッジ付き)を表示する。
 */
@Composable
fun PieceStandView(
    owner: Player,
    viewer: Player,
    name: String,
    isTurn: Boolean,
    hand: Map<PieceType, Int>,
    selected: PieceType?,
    onTap: (Player, PieceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(StandLight, StandDark)), RoundedCornerShape(8.dp))
            .border(
                width = if (isTurn) 2.dp else 1.dp,
                color = if (isTurn) Shu else BoardEdge,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerTag(owner = owner, name = name, isTurn = isTurn)
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            for (type in PieceType.HAND_ORDER) {
                val count = hand[type] ?: 0
                if (count == 0) continue
                HandPiece(
                    type = type,
                    count = count,
                    upsideDown = owner != viewer,
                    selected = type == selected,
                    onClick = { onTap(owner, type) },
                )
            }
        }
    }
}

@Composable
private fun PlayerTag(owner: Player, name: String, isTurn: Boolean) {
    val pulse by rememberInfiniteTransition(label = "turn").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "turnPulse",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
            if (isTurn) {
                Box(
                    Modifier
                        .size(14.dp)
                        .alpha(pulse * 0.5f)
                        .background(Shu, CircleShape),
                )
            }
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (isTurn) Shu else Sumi.copy(alpha = 0.25f), CircleShape),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = (if (owner == Player.SENTE) "☗" else "☖") + name,
            fontFamily = Mincho,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Sumi,
        )
    }
}

@Composable
private fun HandPiece(type: PieceType, count: Int, upsideDown: Boolean, selected: Boolean, onClick: () -> Unit) {
    val lift by animateDpAsState(if (selected) (-8).dp else 0.dp, label = "lift")
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PieceView(
            type = type,
            upsideDown = upsideDown,
            height = 38.dp,
            elevation = if (selected) 10.dp else 2.dp,
            modifier = Modifier.offset(y = lift),
        )
        if (count > 1) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = lift)
                    .size(18.dp)
                    .background(Shu, CircleShape)
                    .border(1.dp, Kincha, CircleShape),
            ) {
                Text("$count", color = Kinari, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
            }
        }
    }
}

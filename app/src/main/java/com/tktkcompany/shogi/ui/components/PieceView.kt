package com.tktkcompany.shogi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tktkcompany.shogi.game.PieceType
import com.tktkcompany.shogi.ui.theme.Mincho
import com.tktkcompany.shogi.ui.theme.PieceDark
import com.tktkcompany.shogi.ui.theme.PieceEdge
import com.tktkcompany.shogi.ui.theme.PieceLight
import com.tktkcompany.shogi.ui.theme.PieceText
import com.tktkcompany.shogi.ui.theme.PromotedText

/** 将棋の駒の五角形。 */
val PieceShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width * 0.86f, size.height * 0.16f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    lineTo(size.width * 0.14f, size.height * 0.16f)
    close()
}

/** 駒の大きさ(マスに対する比率)。本物の駒と同じく玉が大きく歩が小さい。 */
fun pieceScale(type: PieceType): Float = when (type.unpromoted) {
    PieceType.OU -> 0.96f
    PieceType.HI, PieceType.KAKU -> 0.93f
    PieceType.KIN, PieceType.GIN -> 0.89f
    PieceType.KEI -> 0.86f
    PieceType.KYO -> 0.84f
    else -> 0.81f
}

/**
 * 木目調の駒。[height] は駒の高さ。幅は高さの 0.86 倍。
 * [upsideDown] が true なら相手側の向き(180度回転)で表示する。
 */
@Composable
fun PieceView(
    type: PieceType,
    upsideDown: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
) {
    val fontSize = with(LocalDensity.current) { (height * 0.56f).toSp() }
    Box(
        modifier = modifier
            .rotate(if (upsideDown) 180f else 0f)
            .size(width = height * 0.86f, height = height)
            .shadow(elevation, PieceShape)
            .background(Brush.verticalGradient(listOf(PieceLight, PieceDark)), PieceShape)
            .border(0.8.dp, PieceEdge, PieceShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.kanji,
            fontFamily = Mincho,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            lineHeight = fontSize,
            color = if (type.isPromoted) PromotedText else PieceText,
            modifier = Modifier.padding(top = height * 0.1f),
        )
    }
}

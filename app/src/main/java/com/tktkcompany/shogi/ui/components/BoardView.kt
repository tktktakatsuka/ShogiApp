package com.tktkcompany.shogi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tktkcompany.shogi.R
import com.tktkcompany.shogi.game.Move
import com.tktkcompany.shogi.game.Player
import com.tktkcompany.shogi.game.Position
import com.tktkcompany.shogi.game.Square
import com.tktkcompany.shogi.ui.theme.AiHint
import com.tktkcompany.shogi.ui.theme.BoardDark
import com.tktkcompany.shogi.ui.theme.BoardEdge
import com.tktkcompany.shogi.ui.theme.BoardGrain
import com.tktkcompany.shogi.ui.theme.BoardLight
import com.tktkcompany.shogi.ui.theme.BoardLine
import com.tktkcompany.shogi.ui.theme.LastMoveSquare
import com.tktkcompany.shogi.ui.theme.Mincho
import com.tktkcompany.shogi.ui.theme.SelectedSquare
import com.tktkcompany.shogi.ui.theme.TargetDot
import kotlin.random.Random

/** 木目の筋 (y 位置・傾き・太さ・濃さ。位置は 0〜1 の比率)。 */
private class Grain(val y: Float, val slope: Float, val width: Float, val alpha: Float)

private val grains: List<Grain> = Random(7).let { r ->
    List(36) {
        Grain(
            y = r.nextFloat(),
            slope = (r.nextFloat() - 0.5f) * 0.06f,
            width = 0.6f + r.nextFloat() * 2.2f,
            alpha = 0.05f + r.nextFloat() * 0.12f,
        )
    }
}

/** 榧(かや)風の木目。 */
private fun Modifier.woodGrain(): Modifier = this
    .background(Brush.linearGradient(listOf(BoardLight, BoardDark, BoardLight, BoardDark)))
    .drawBehind {
        for (g in grains) {
            val y = g.y * size.height
            drawLine(
                color = BoardGrain.copy(alpha = g.alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y + g.slope * size.height),
                strokeWidth = g.width,
            )
        }
    }

/**
 * 将棋盤。マス目・星・筋と段の数字・ハイライト・駒の移動アニメーションを描く。
 * [viewer] が手前側(下)に来るように表示する。
 */
@Composable
fun BoardView(
    position: Position,
    viewer: Player,
    selected: Square?,
    targets: Set<Square>,
    lastMove: Move?,
    moveCount: Int,
    onSquareTapped: (Square) -> Unit,
    modifier: Modifier = Modifier,
    /** 矢印で示す手(感想戦でのAIの最善手)。 */
    hintMove: Move? = null,
) {
    val flipped = viewer == Player.GOTE
    val onTap by rememberUpdatedState(onSquareTapped)

    // 手数が増えたときだけ直前の手をアニメーションさせる(待ったで戻したときは動かさない)
    val previousCount = remember { intArrayOf(moveCount) }
    val progress = remember(moveCount) { Animatable(if (moveCount > previousCount[0]) 0f else 1f) }
    SideEffect { previousCount[0] = moveCount }
    LaunchedEffect(progress) { progress.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .border(3.dp, BoardEdge, RoundedCornerShape(4.dp))
            .woodGrain(),
    ) {
        val pad = 6.dp
        val label = 14.dp
        val cell: Dp = (maxWidth - pad * 2 - label) / 9
        val gridSize = cell * 9

        fun displayCol(sq: Square) = if (flipped) 8 - sq.col else sq.col
        fun displayRow(sq: Square) = if (flipped) 8 - sq.row else sq.row

        Box(Modifier.height(pad * 2 + label + gridSize))

        // 筋(上)と段(右)の数字
        val fileLabels = stringResource(R.string.board_files)
        val rankLabels = stringResource(R.string.board_ranks)
        for (i in 0..8) {
            val fileText = fileLabels[if (flipped) 8 - i else i].toString()
            val rankText = rankLabels[if (flipped) 8 - i else i].toString()
            BoardLabel(fileText, Modifier.offset(x = pad + cell * i, y = pad).size(cell, label))
            BoardLabel(
                rankText,
                Modifier.offset(x = pad + gridSize, y = pad + label + cell * i).size(label, cell),
            )
        }

        Box(
            modifier = Modifier
                .offset(x = pad, y = pad + label)
                .size(gridSize)
                .drawBehind {
                    val c = size.width / 9
                    for (i in 0..9) {
                        val w = if (i == 0 || i == 9) 2.5f else 1.2f
                        drawLine(BoardLine, Offset(c * i, 0f), Offset(c * i, size.height), w)
                        drawLine(BoardLine, Offset(0f, c * i), Offset(size.width, c * i), w)
                    }
                    // 星
                    for (x in listOf(3, 6)) for (y in listOf(3, 6)) {
                        drawCircle(BoardLine, radius = c * 0.06f, center = Offset(c * x, c * y))
                    }
                }
                .pointerInput(flipped, gridSize) {
                    detectTapGestures { tap ->
                        val c = size.width / 9f
                        val dc = (tap.x / c).toInt().coerceIn(0, 8)
                        val dr = (tap.y / c).toInt().coerceIn(0, 8)
                        onTap(if (flipped) Square(8 - dr, 8 - dc) else Square(dr, dc))
                    }
                },
        ) {
            // ハイライト
            val highlightSquares = buildList {
                lastMove?.from?.let { add(it to LastMoveSquare) }
                lastMove?.to?.let { add(it to LastMoveSquare) }
                selected?.let { add(it to SelectedSquare) }
            }
            for ((sq, color) in highlightSquares) {
                Box(
                    Modifier
                        .offset(x = cell * displayCol(sq), y = cell * displayRow(sq))
                        .size(cell)
                        .background(color),
                )
            }

            // 駒
            for (sq in Square.ALL) {
                val piece = position[sq] ?: continue
                val animating = lastMove != null && sq == lastMove.to && progress.value < 1f
                val p = progress.value
                val from = lastMove?.from
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .zIndex(if (animating) 1f else 0f)
                        .offset {
                            val cellPx = cell.toPx()
                            var x = displayCol(sq) * cellPx
                            var y = displayRow(sq) * cellPx
                            if (animating && from != null) {
                                x += (displayCol(from) - displayCol(sq)) * cellPx * (1 - p)
                                y += (displayRow(from) - displayRow(sq)) * cellPx * (1 - p)
                            }
                            IntOffset(x.toInt(), y.toInt())
                        }
                        .size(cell)
                        .graphicsLayer {
                            // 打った駒は少し大きい状態から盤に置かれる
                            if (animating && from == null) {
                                val s = 1.35f - 0.35f * p
                                scaleX = s
                                scaleY = s
                                alpha = p
                            }
                        },
                ) {
                    PieceView(
                        type = piece.type,
                        upsideDown = piece.owner != viewer,
                        height = cell * pieceScale(piece.type),
                        elevation = if (animating) 8.dp else 2.dp,
                    )
                }
            }

            // 移動できるマス(点で表示)
            for (sq in targets) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .zIndex(2f)
                        .offset(x = cell * displayCol(sq), y = cell * displayRow(sq))
                        .size(cell),
                ) {
                    val dot = if (position[sq] != null) cell * 0.42f else cell * 0.28f
                    Box(
                        Modifier
                            .size(dot)
                            .background(TargetDot, CircleShape),
                    )
                }
            }

            // AIの最善手(動かす手は矢印、打つ手は打つマスを丸で囲む)
            if (hintMove != null) {
                val from = hintMove.from
                Box(
                    Modifier
                        .matchParentSize()
                        .zIndex(3f)
                        .drawBehind {
                            val c = size.width / 9
                            fun center(sq: Square) =
                                Offset((displayCol(sq) + 0.5f) * c, (displayRow(sq) + 0.5f) * c)
                            val color = AiHint.copy(alpha = 0.8f)
                            val end = center(hintMove.to)
                            if (from == null) {
                                drawCircle(color, radius = c * 0.42f, center = end, style = Stroke(c * 0.1f))
                                return@drawBehind
                            }
                            val start = center(from)
                            val length = (end - start).getDistance()
                            val dir = (end - start) / length
                            val side = Offset(-dir.y, dir.x)
                            val head = c * 0.34f
                            val neck = end - dir * head
                            drawLine(color, start + dir * (c * 0.2f), neck, strokeWidth = c * 0.14f)
                            drawPath(
                                Path().apply {
                                    moveTo(end.x, end.y)
                                    (neck + side * (head * 0.6f)).let { lineTo(it.x, it.y) }
                                    (neck - side * (head * 0.6f)).let { lineTo(it.x, it.y) }
                                    close()
                                },
                                color,
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun BoardLabel(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            fontFamily = Mincho,
            fontWeight = FontWeight.Bold,
            color = BoardLine,
            fontSize = 10.sp,
        )
    }
}

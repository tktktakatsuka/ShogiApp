package com.tktkcompany.shogi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tktkcompany.shogi.game.Piece
import com.tktkcompany.shogi.game.PieceType
import com.tktkcompany.shogi.game.Player
import com.tktkcompany.shogi.game.Square
import com.tktkcompany.shogi.ui.theme.BoardLine
import com.tktkcompany.shogi.ui.theme.BoardWood
import com.tktkcompany.shogi.ui.theme.LastMoveSquare
import com.tktkcompany.shogi.ui.theme.PieceText
import com.tktkcompany.shogi.ui.theme.PromotedText
import com.tktkcompany.shogi.ui.theme.SelectedSquare
import com.tktkcompany.shogi.ui.theme.StandWood
import com.tktkcompany.shogi.ui.theme.TargetSquare

@Composable
fun GameScreen(viewModel: ShogiViewModel) {
    val state = viewModel.state
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmResign by remember { mutableStateOf(false) }
    val leaveNeedsConfirm = state.mode == GameMode.ONLINE && state.result == null

    BackHandler {
        if (leaveNeedsConfirm) confirmLeave = true else viewModel.backToHome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.online?.let {
            Text(
                "オンライン対戦　部屋 ${it.code}　あなたは${it.myColor.label}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        StatusBar(state)
        val top = state.viewer.opponent
        val bottom = state.viewer
        PieceStand(
            owner = top,
            viewer = state.viewer,
            hand = state.position.handOf(top),
            selected = (state.selection as? Selection.InHand)?.type?.takeIf { state.position.turn == top },
            onTap = viewModel::onHandPieceTapped,
        )
        Board(state, onSquareTapped = viewModel::onSquareTapped)
        PieceStand(
            owner = bottom,
            viewer = state.viewer,
            hand = state.position.handOf(bottom),
            selected = (state.selection as? Selection.InHand)?.type?.takeIf { state.position.turn == bottom },
            onTap = viewModel::onHandPieceTapped,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.mode != GameMode.ONLINE) {
                OutlinedButton(onClick = viewModel::undo, enabled = viewModel.canUndo()) { Text("待った") }
            }
            OutlinedButton(onClick = { confirmResign = true }, enabled = state.result == null) { Text("投了") }
            if (state.mode != GameMode.ONLINE) {
                Button(onClick = { viewModel.startLocalGame(state.mode) }) { Text("新規対局") }
            }
        }
        TextButton(onClick = { if (leaveNeedsConfirm) confirmLeave = true else viewModel.backToHome() }) {
            Text("メニューに戻る")
        }
    }

    state.pendingPromotion?.let {
        AlertDialog(
            onDismissRequest = viewModel::onPromotionCancelled,
            title = { Text("成りますか？") },
            confirmButton = {
                Button(onClick = { viewModel.onPromotionChosen(true) }) { Text("成る") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.onPromotionChosen(false) }) { Text("成らない") }
            },
        )
    }

    val result = state.result
    if (state.showResultDialog && result != null) {
        val online = state.online
        val title = when {
            online == null || result.winner == null -> "対局終了"
            result.winner == online.myColor -> "あなたの勝ち！"
            else -> "あなたの負け"
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text(title) },
            text = { Text("${state.moveCount}手で${result.message}") },
            confirmButton = {
                if (state.mode == GameMode.ONLINE) {
                    Button(onClick = viewModel::backToHome) { Text("メニューへ") }
                } else {
                    Button(onClick = { viewModel.startLocalGame(state.mode) }) { Text("もう一局") }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResult) { Text("盤面を見る") }
            },
        )
    }

    if (confirmResign) {
        AlertDialog(
            onDismissRequest = { confirmResign = false },
            title = { Text("投了しますか？") },
            confirmButton = {
                Button(onClick = { confirmResign = false; viewModel.resign() }) { Text("投了する") }
            },
            dismissButton = {
                TextButton(onClick = { confirmResign = false }) { Text("続ける") }
            },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("対局をやめますか？") },
            text = { Text("対局中に退出すると投了になります。") },
            confirmButton = {
                Button(onClick = { confirmLeave = false; viewModel.backToHome() }) { Text("退出する") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text("続ける") }
            },
        )
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("エラー") },
            text = { Text(message) },
            confirmButton = { Button(onClick = viewModel::dismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun StatusBar(state: UiState) {
    val online = state.online
    val result = state.result
    val text = when {
        result != null -> "対局終了：${result.message}"
        state.cpuThinking -> "CPUが考えています…"
        else -> buildString {
            append("${state.moveCount + 1}手目　")
            when {
                online == null -> append("${state.position.turn.label}の番")
                state.position.turn == online.myColor -> append("あなたの番です")
                else -> append("相手の番です")
            }
            if (state.inCheck) append("　王手！")
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (state.inCheck && result == null) PromotedText else Color.Unspecified,
    )
}

@Composable
private fun Board(state: UiState, onSquareTapped: (Square) -> Unit) {
    val selectedSquare = (state.selection as? Selection.OnBoard)?.square
    val lastTo = state.lastMove?.to
    val lastFrom = state.lastMove?.from
    // 後手視点では盤を180度回転して表示する
    val flipped = state.viewer == Player.GOTE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(BoardWood)
            .border(2.dp, BoardLine),
    ) {
        for (displayRow in 0..8) {
            Row(modifier = Modifier.weight(1f)) {
                for (displayCol in 0..8) {
                    val sq = if (flipped) Square(8 - displayRow, 8 - displayCol) else Square(displayRow, displayCol)
                    val highlight = when (sq) {
                        selectedSquare -> SelectedSquare
                        in state.targets -> TargetSquare
                        lastTo, lastFrom -> LastMoveSquare
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .border(0.5.dp, BoardLine)
                            .background(highlight)
                            .clickable { onSquareTapped(sq) },
                        contentAlignment = Alignment.Center,
                    ) {
                        state.position[sq]?.let { PieceLabel(it, upsideDown = it.owner != state.viewer) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceLabel(piece: Piece, upsideDown: Boolean, fontSizeDp: Float = 22f) {
    val fontSize = with(LocalDensity.current) { fontSizeDp.dp.toSp() }
    Text(
        text = piece.type.kanji,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = if (piece.type.isPromoted) PromotedText else PieceText,
        modifier = if (upsideDown) Modifier.rotate(180f) else Modifier,
    )
}

@Composable
private fun PieceStand(
    owner: Player,
    viewer: Player,
    hand: Map<PieceType, Int>,
    selected: PieceType?,
    onTap: (Player, PieceType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StandWood, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (owner == Player.SENTE) "☗先手" else "☖後手",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(4.dp))
        for (type in PieceType.HAND_ORDER) {
            val count = hand[type] ?: 0
            if (count == 0) continue
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (type == selected) SelectedSquare else Color.Transparent,
                        RoundedCornerShape(4.dp),
                    )
                    .clickable { onTap(owner, type) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    PieceLabel(Piece(type, owner), upsideDown = owner != viewer, fontSizeDp = 20f)
                    if (count > 1) Text("$count", fontSize = 12.sp)
                }
            }
        }
    }
}

package com.tktkcompany.shogi.ui

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.tktkcompany.shogi.ui.theme.ShogiTheme
import com.tktkcompany.shogi.ui.theme.StandWood
import com.tktkcompany.shogi.ui.theme.TargetSquare

@Composable
fun ShogiScreen(viewModel: ShogiViewModel = viewModel()) {
    val state = viewModel.state
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusBar(state)
            PieceStand(
                owner = Player.GOTE,
                hand = state.position.handOf(Player.GOTE),
                selected = (state.selection as? Selection.InHand)?.type
                    ?.takeIf { state.position.turn == Player.GOTE },
                onTap = viewModel::onHandPieceTapped,
            )
            Board(state, onSquareTapped = viewModel::onSquareTapped)
            PieceStand(
                owner = Player.SENTE,
                hand = state.position.handOf(Player.SENTE),
                selected = (state.selection as? Selection.InHand)?.type
                    ?.takeIf { state.position.turn == Player.SENTE },
                onTap = viewModel::onHandPieceTapped,
            )
            Spacer(Modifier.weight(1f))
            Controls(
                mode = state.mode,
                onUndo = viewModel::undo,
                onNewGame = viewModel::newGame,
            )
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

    state.resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("対局終了") },
            text = { Text("${state.moveCount}手で$message") },
            confirmButton = {
                Button(onClick = { viewModel.newGame() }) { Text("もう一局") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResult) { Text("盤面を見る") }
            },
        )
    }
}

@Composable
private fun StatusBar(state: UiState) {
    val text = when {
        state.resultMessage != null -> "対局終了：${state.resultMessage}"
        state.cpuThinking -> "CPUが考えています…"
        else -> buildString {
            append("${state.moveCount + 1}手目　${state.position.turn.label}の番")
            if (state.inCheck) append("　王手！")
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (state.inCheck && state.resultMessage == null) PromotedText else Color.Unspecified,
    )
}

@Composable
private fun Board(state: UiState, onSquareTapped: (Square) -> Unit) {
    val selectedSquare = (state.selection as? Selection.OnBoard)?.square
    val lastTo = state.lastMove?.to
    val lastFrom = state.lastMove?.from
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(BoardWood)
            .border(2.dp, BoardLine),
    ) {
        for (row in 0..8) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..8) {
                    val sq = Square(row, col)
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
                        state.position[sq]?.let { PieceLabel(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceLabel(piece: Piece, fontSizeDp: Float = 22f) {
    val fontSize = with(LocalDensity.current) { fontSizeDp.dp.toSp() }
    Text(
        text = piece.type.kanji,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = if (piece.type.isPromoted) PromotedText else PieceText,
        modifier = if (piece.owner == Player.GOTE) Modifier.rotate(180f) else Modifier,
    )
}

@Composable
private fun PieceStand(
    owner: Player,
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
                    PieceLabel(Piece(type, owner), fontSizeDp = 20f)
                    if (count > 1) Text("$count", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Controls(mode: GameMode, onUndo: () -> Unit, onNewGame: (GameMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUndo) { Text("待った") }
            Button(onClick = { onNewGame(mode) }) { Text("新規対局") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in GameMode.entries) {
                TextButton(onClick = { onNewGame(m) }, enabled = m != mode) {
                    Text(if (m == mode) "● ${m.label}" else m.label)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ShogiScreenPreview() {
    ShogiTheme { ShogiScreen() }
}

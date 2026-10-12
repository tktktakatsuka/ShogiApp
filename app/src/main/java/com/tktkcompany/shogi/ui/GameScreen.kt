package com.tktkcompany.shogi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tktkcompany.shogi.ui.components.BoardView
import com.tktkcompany.shogi.ui.components.PieceStandView
import com.tktkcompany.shogi.ui.components.ShuButton
import com.tktkcompany.shogi.ui.components.SumiButton
import com.tktkcompany.shogi.ui.components.washiBackground
import com.tktkcompany.shogi.ui.theme.Kinari
import com.tktkcompany.shogi.ui.theme.Mincho
import com.tktkcompany.shogi.ui.theme.Shu
import com.tktkcompany.shogi.ui.theme.Sumi
import kotlinx.coroutines.delay

@Composable
fun GameScreen(viewModel: ShogiViewModel) {
    val state = viewModel.state
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmResign by remember { mutableStateOf(false) }
    val leaveNeedsConfirm = state.mode == GameMode.ONLINE && state.result == null
    val leave: () -> Unit = { if (leaveNeedsConfirm) confirmLeave = true else viewModel.backToHome() }

    BackHandler(onBack = leave)
    MoveFeedback(moveCount = state.moveCount, enabled = viewModel.soundEnabled)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .washiBackground()
            .safeDrawingPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TopBar(
            title = state.online?.let { "部屋 ${it.code}" } ?: state.mode.label,
            soundEnabled = viewModel.soundEnabled,
            onToggleSound = viewModel::toggleSound,
            onBack = leave,
        )
        StatusBar(state)

        val top = state.viewer.opponent
        val bottom = state.viewer
        val handSelection = (state.selection as? Selection.InHand)?.type
        PieceStandView(
            owner = top,
            viewer = state.viewer,
            name = state.nameOf(top),
            isTurn = state.result == null && state.position.turn == top,
            hand = state.position.handOf(top),
            selected = handSelection?.takeIf { state.position.turn == top },
            onTap = viewModel::onHandPieceTapped,
        )
        Box(contentAlignment = Alignment.Center) {
            BoardView(
                position = state.position,
                viewer = state.viewer,
                selected = (state.selection as? Selection.OnBoard)?.square,
                targets = state.targets,
                lastMove = state.lastMove,
                moveCount = state.moveCount,
                onSquareTapped = viewModel::onSquareTapped,
            )
            CheckFlash(moveCount = state.moveCount, inCheck = state.inCheck && state.result == null)
        }
        PieceStandView(
            owner = bottom,
            viewer = state.viewer,
            name = state.nameOf(bottom),
            isTurn = state.result == null && state.position.turn == bottom,
            hand = state.position.handOf(bottom),
            selected = handSelection?.takeIf { state.position.turn == bottom },
            onTap = viewModel::onHandPieceTapped,
        )
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.mode != GameMode.ONLINE) {
                SumiButton("待った", Modifier.weight(1f), enabled = viewModel.canUndo(), onClick = viewModel::undo)
            }
            SumiButton("投了", Modifier.weight(1f), enabled = state.result == null) { confirmResign = true }
            if (state.mode != GameMode.ONLINE) {
                ShuButton("新規対局", Modifier.weight(1f)) { viewModel.startLocalGame(state.mode) }
            }
        }
    }

    GameDialogs(
        viewModel = viewModel,
        confirmResign = confirmResign,
        onConfirmResignChange = { confirmResign = it },
        confirmLeave = confirmLeave,
        onConfirmLeaveChange = { confirmLeave = it },
    )
}

/** 駒を指したときの効果音と振動。 */
@Composable
private fun MoveFeedback(moveCount: Int, enabled: Boolean) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val player = remember { SoundPlayer(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val previous = remember { intArrayOf(moveCount) }
    LaunchedEffect(moveCount) {
        if (enabled && moveCount > previous[0]) {
            player.playKoma()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previous[0] = moveCount
    }
}

@Composable
private fun TopBar(title: String, soundEnabled: Boolean, onToggleSound: () -> Unit, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onBack) { Text("‹ メニュー", color = Sumi, fontFamily = Mincho) }
        Text(
            title,
            fontFamily = Mincho,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Sumi,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(onClick = onToggleSound) {
            Text(if (soundEnabled) "音 あり" else "音 なし", color = Sumi, fontFamily = Mincho)
        }
    }
}

@Composable
private fun StatusBar(state: UiState) {
    val online = state.online
    val result = state.result
    val text = when {
        result != null -> "対局終了 ― ${result.message}"
        state.cpuThinking -> "CPUが考えています…"
        else -> buildString {
            append("${state.moveCount + 1}手目　")
            when {
                online == null && state.mode == GameMode.VS_HUMAN -> append("${state.position.turn.label}の番")
                state.nameOf(state.position.turn) == "あなた" -> append("あなたの番です")
                else -> append("${state.nameOf(state.position.turn)}の番です")
            }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            fontFamily = Mincho,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = if (state.inCheck && result == null) Shu else Sumi,
        )
        Text(
            text = state.lastMoveNotation?.let { "直前の手　$it" } ?: "対局開始",
            fontFamily = Mincho,
            fontSize = 14.sp,
            color = Sumi.copy(alpha = 0.75f),
        )
    }
}

/** 王手がかかった瞬間に「王手」を大きく表示する。 */
@Composable
private fun CheckFlash(moveCount: Int, inCheck: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(moveCount) {
        if (inCheck) {
            visible = true
            delay(900)
        }
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 1.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
    ) {
        Text(
            "王手",
            style = TextStyle(
                fontFamily = Mincho,
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                color = Shu,
                shadow = Shadow(color = Kinari, offset = Offset(0f, 0f), blurRadius = 18f),
            ),
        )
    }
}

@Composable
private fun GameDialogs(
    viewModel: ShogiViewModel,
    confirmResign: Boolean,
    onConfirmResignChange: (Boolean) -> Unit,
    confirmLeave: Boolean,
    onConfirmLeaveChange: (Boolean) -> Unit,
) {
    val state = viewModel.state

    state.pendingPromotion?.let {
        AlertDialog(
            onDismissRequest = viewModel::onPromotionCancelled,
            title = { Text("成りますか？") },
            confirmButton = {
                ShuButton("成る") { viewModel.onPromotionChosen(true) }
            },
            dismissButton = {
                SumiButton("成らない") { viewModel.onPromotionChosen(false) }
            },
        )
    }

    val result = state.result
    if (state.showResultDialog && result != null) {
        val online = state.online
        val title = when {
            result.winner == null -> "引き分け"
            state.mode == GameMode.VS_HUMAN -> "${result.winner.label}の勝ち"
            state.nameOf(result.winner) == "あなた" -> "勝ち！"
            else -> "負け"
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = {
                Text(
                    title,
                    fontFamily = Mincho,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = if (title == "勝ち！") Shu else Sumi,
                )
            },
            text = { Text("${state.moveCount}手で${result.message}", fontFamily = Mincho) },
            confirmButton = {
                if (online != null) {
                    ShuButton("メニューへ", onClick = viewModel::backToHome)
                } else {
                    ShuButton("もう一局") { viewModel.startLocalGame(state.mode) }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResult) { Text("盤面を見る", color = Sumi) }
            },
        )
    }

    if (confirmResign) {
        AlertDialog(
            onDismissRequest = { onConfirmResignChange(false) },
            title = { Text("投了しますか？") },
            confirmButton = {
                ShuButton("投了する") {
                    onConfirmResignChange(false)
                    viewModel.resign()
                }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmResignChange(false) }) { Text("続ける", color = Sumi) }
            },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { onConfirmLeaveChange(false) },
            title = { Text("対局をやめますか？") },
            text = { Text("対局中に退出すると投了になります。") },
            confirmButton = {
                ShuButton("退出する") {
                    onConfirmLeaveChange(false)
                    viewModel.backToHome()
                }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmLeaveChange(false) }) { Text("続ける", color = Sumi) }
            },
        )
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("エラー") },
            text = { Text(message) },
            confirmButton = { ShuButton("OK", onClick = viewModel::dismissError) },
        )
    }
}

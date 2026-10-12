package com.tktkcompany.shogi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tktkcompany.shogi.R
import com.tktkcompany.shogi.ui.components.AdBanner
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

/** 感想戦で盤以外に必要な高さの目安(上部の表示・駒台・操作ボタンと、チャット欄を使える最低限の高さ)。 */
private val REVIEW_RESERVED_HEIGHT = 560.dp

/** 感想戦でAIの最善手を表示しているときに増える高さ。 */
private val REVIEW_AI_HEIGHT = 54.dp

@Composable
fun GameScreen(viewModel: ShogiViewModel) {
    val state = viewModel.state
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmResign by remember { mutableStateOf(false) }
    val leaveNeedsConfirm = state.mode == GameMode.ONLINE && state.result == null
    val leave: () -> Unit = {
        when {
            // 感想戦中は対局終了の盤面に戻る
            state.review != null -> viewModel.exitReview()
            leaveNeedsConfirm -> confirmLeave = true
            else -> viewModel.backToHome()
        }
    }

    BackHandler(onBack = leave)
    MoveFeedback(moveCount = state.moveCount, enabled = viewModel.soundEnabled)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .washiBackground()
            .safeDrawingPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val review = state.review
            TopBar(
                title = when {
                    // 感想戦では場所を節約するため、手数もここに表示する
                    review == null ->
                        state.online?.let { stringResource(R.string.room_title, it.code) } ?: stringResource(state.mode.label)
                    review.isVariation -> stringResource(
                        R.string.review_title_variation,
                        review.index,
                        review.total,
                        review.variationNotations.size,
                    )
                    else -> stringResource(R.string.review_title, review.index, review.total)
                },
                backLabel = stringResource(if (review != null) R.string.back_exit_review else R.string.back_menu),
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
                isTurn = (review != null || state.result == null) && state.position.turn == top,
                hand = state.position.handOf(top),
                selected = handSelection?.takeIf { state.position.turn == top },
                onTap = viewModel::onHandPieceTapped,
                compact = review != null,
            )
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                // 対局中は、画面が短い端末でもボタンと広告が収まるよう、残りの高さに合わせて盤を小さくする
                modifier = if (review != null) Modifier else Modifier.weight(1f, fill = false),
            ) {
                BoardView(
                    modifier = if (review != null) {
                        // 感想戦ではチャット欄の場所を空けるため盤を少し小さくする。
                        // 画面が短い端末ではさらに小さくする(駒を押しにくくならない幅までにとどめる)
                        val screen = LocalConfiguration.current
                        val reserved = REVIEW_RESERVED_HEIGHT + if (state.ai != null) REVIEW_AI_HEIGHT else 0.dp
                        Modifier.width((screen.screenHeightDp.dp - reserved).coerceIn(maxWidth * 0.6f, maxWidth * 0.8f))
                    } else {
                        Modifier.width(minOf(maxWidth, maxHeight))
                    },
                    position = state.position,
                    viewer = state.viewer,
                    selected = (state.selection as? Selection.OnBoard)?.square,
                    targets = state.targets,
                    lastMove = state.lastMove,
                    hintMove = state.ai?.bestMove,
                    moveCount = state.moveCount,
                    onSquareTapped = viewModel::onSquareTapped,
                )
                CheckFlash(moveCount = state.moveCount, inCheck = state.inCheck && state.result == null)
            }
            PieceStandView(
                owner = bottom,
                viewer = state.viewer,
                name = state.nameOf(bottom),
                isTurn = (review != null || state.result == null) && state.position.turn == bottom,
                hand = state.position.handOf(bottom),
                selected = handSelection?.takeIf { state.position.turn == bottom },
                onTap = viewModel::onHandPieceTapped,
                compact = review != null,
            )
            if (review != null) {
                ReviewControls(review, state.ai, viewModel)
                ReviewBottomPanel(state, viewModel, Modifier.weight(1f))
                return@Column
            }
            if (state.partnerInReview) {
                Text(
                    stringResource(R.string.partner_started_review),
                    fontFamily = Mincho,
                    color = Shu,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (state.result != null) {
                ShuButton(
                    stringResource(if (state.online != null) R.string.review_with_chat else R.string.review),
                    Modifier.fillMaxWidth(),
                    onClick = viewModel::startReview,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.mode != GameMode.ONLINE) {
                    SumiButton(
                        stringResource(R.string.undo),
                        Modifier.weight(1f),
                        enabled = viewModel.canUndo(),
                        onClick = viewModel::undo,
                    )
                }
                SumiButton(stringResource(R.string.resign), Modifier.weight(1f), enabled = state.result == null) {
                    confirmResign = true
                }
                if (state.mode != GameMode.ONLINE) {
                    ShuButton(stringResource(R.string.new_game), Modifier.weight(1f)) {
                        viewModel.startLocalGame(state.mode)
                    }
                }
            }
        }
        // 広告は画面の一番下に置く。感想戦中は棋譜とチャットの場所を取るため表示しない
        if (state.review == null) AdBanner(Modifier.padding(top = 8.dp))
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
private fun TopBar(
    title: String,
    backLabel: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onBack) { Text(backLabel, color = Sumi, fontFamily = Mincho) }
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
            Text(
                stringResource(if (soundEnabled) R.string.sound_on else R.string.sound_off),
                color = Sumi,
                fontFamily = Mincho,
            )
        }
    }
}

@Composable
private fun StatusBar(state: UiState) {
    val result = state.result
    val review = state.review
    val turn = state.position.turn
    val text = when {
        // 感想戦の手数は上のバーに表示している
        review != null -> null
        result != null -> stringResource(R.string.status_game_over, result.message())
        state.cpuThinking -> stringResource(R.string.status_cpu_thinking)
        state.mode == GameMode.VS_HUMAN ->
            stringResource(R.string.status_turn_player, state.moveCount + 1, state.nameOf(turn))
        state.isMe(turn) -> stringResource(R.string.status_turn_you, state.moveCount + 1)
        else -> stringResource(R.string.status_turn_other, state.moveCount + 1, state.nameOf(turn))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (text != null) {
            Text(
                text = text,
                fontFamily = Mincho,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (state.inCheck && result == null) Shu else Sumi,
            )
        }
        Text(
            text = state.lastMoveNotation?.let { stringResource(R.string.last_move, it) }
                ?: stringResource(R.string.game_start),
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
            stringResource(R.string.check),
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
            title = { Text(stringResource(R.string.promote_title)) },
            confirmButton = {
                ShuButton(stringResource(R.string.promote_yes)) { viewModel.onPromotionChosen(true) }
            },
            dismissButton = {
                SumiButton(stringResource(R.string.promote_no)) { viewModel.onPromotionChosen(false) }
            },
        )
    }

    val result = state.result
    if (state.showResultDialog && result != null) {
        val online = state.online
        val won = result.winner != null && state.isMe(result.winner)
        val title = when {
            result.winner == null -> stringResource(R.string.result_draw)
            state.mode == GameMode.VS_HUMAN ->
                stringResource(R.string.result_player_wins, stringResource(result.winner.label))
            won -> stringResource(R.string.result_you_win)
            else -> stringResource(R.string.result_you_lose)
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = {
                Text(
                    title,
                    fontFamily = Mincho,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = if (won) Shu else Sumi,
                )
            },
            text = {
                Text(stringResource(R.string.result_summary, state.moveCount, result.message()), fontFamily = Mincho)
            },
            confirmButton = {
                ShuButton(stringResource(R.string.review), onClick = viewModel::startReview)
            },
            dismissButton = {
                Row {
                    if (online != null) {
                        TextButton(onClick = viewModel::backToHome) {
                            Text(stringResource(R.string.to_menu), color = Sumi)
                        }
                    } else {
                        TextButton(onClick = { viewModel.startLocalGame(state.mode) }) {
                            Text(stringResource(R.string.play_again), color = Sumi)
                        }
                    }
                    TextButton(onClick = viewModel::dismissResult) {
                        Text(stringResource(R.string.close), color = Sumi)
                    }
                }
            },
        )
    }

    if (confirmResign) {
        AlertDialog(
            onDismissRequest = { onConfirmResignChange(false) },
            title = { Text(stringResource(R.string.resign_title)) },
            confirmButton = {
                ShuButton(stringResource(R.string.resign_confirm)) {
                    onConfirmResignChange(false)
                    viewModel.resign()
                }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmResignChange(false) }) {
                    Text(stringResource(R.string.keep_playing), color = Sumi)
                }
            },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { onConfirmLeaveChange(false) },
            title = { Text(stringResource(R.string.leave_title)) },
            text = { Text(stringResource(R.string.leave_text)) },
            confirmButton = {
                ShuButton(stringResource(R.string.leave_confirm)) {
                    onConfirmLeaveChange(false)
                    viewModel.backToHome()
                }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmLeaveChange(false) }) {
                    Text(stringResource(R.string.keep_playing), color = Sumi)
                }
            },
        )
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(message.resolve()) },
            confirmButton = { ShuButton(stringResource(R.string.ok), onClick = viewModel::dismissError) },
        )
    }
}

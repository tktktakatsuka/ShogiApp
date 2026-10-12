package com.tktkcompany.shogi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tktkcompany.shogi.game.PieceType
import com.tktkcompany.shogi.ui.components.AdBanner
import com.tktkcompany.shogi.ui.components.PieceView
import com.tktkcompany.shogi.ui.components.ShuButton
import com.tktkcompany.shogi.ui.components.SumiButton
import com.tktkcompany.shogi.ui.components.washiBackground
import com.tktkcompany.shogi.ui.theme.Kincha
import com.tktkcompany.shogi.ui.theme.Kinari
import com.tktkcompany.shogi.ui.theme.Mincho
import com.tktkcompany.shogi.ui.theme.Shu
import com.tktkcompany.shogi.ui.theme.Sumi

/** アプリ全体。画面(ホーム / オンラインロビー / 対局)を切り替える。 */
@Composable
fun ShogiApp(viewModel: ShogiViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = Kinari, contentColor = Sumi) {
        AppContent(viewModel)
    }
}

@Composable
private fun AppContent(viewModel: ShogiViewModel) {
    AnimatedContent(
        targetState = viewModel.screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
        modifier = Modifier.fillMaxSize(),
    ) { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(
                onlineAvailable = viewModel.onlineAvailable,
                onlineProblem = viewModel.onlineProblem,
                soundEnabled = viewModel.soundEnabled,
                onToggleSound = viewModel::toggleSound,
                onStart = viewModel::startLocalGame,
                onOnline = viewModel::openLobby,
            )
            Screen.LOBBY -> {
                BackHandler(onBack = viewModel::backToHome)
                LobbyScreen(
                    lobby = viewModel.lobby,
                    onCreate = viewModel::createRoom,
                    onJoin = viewModel::joinRoom,
                    onCancelWaiting = viewModel::cancelWaiting,
                    onBack = viewModel::backToHome,
                )
            }
            Screen.GAME -> GameScreen(viewModel)
        }
    }
}

@Composable
private fun HomeScreen(
    onlineAvailable: Boolean,
    onlineProblem: String?,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onStart: (GameMode) -> Unit,
    onOnline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .washiBackground()
            .safeDrawingPadding(),
    ) {
        HomeMenu(onlineAvailable, onlineProblem, soundEnabled, onToggleSound, onStart, onOnline, Modifier.weight(1f))
        AdBanner()
    }
}

@Composable
private fun HomeMenu(
    onlineAvailable: Boolean,
    onlineProblem: String?,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onStart: (GameMode) -> Unit,
    onOnline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Logo()
        Spacer(Modifier.height(20.dp))
        ShuButton(GameMode.VS_CPU.label, Modifier.fillMaxWidth(), height = 56.dp) { onStart(GameMode.VS_CPU) }
        ShuButton(GameMode.VS_HUMAN.label, Modifier.fillMaxWidth(), height = 56.dp) { onStart(GameMode.VS_HUMAN) }
        ShuButton(
            GameMode.ONLINE.label,
            Modifier.fillMaxWidth(),
            enabled = onlineAvailable,
            height = 56.dp,
            onClick = onOnline,
        )
        if (!onlineAvailable) {
            Text(
                onlineProblem ?: "オンライン対戦を使うには Firebase の設定(google-services.json)が必要です",
                fontSize = 12.sp,
                color = Shu,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("効果音・振動", fontFamily = Mincho, color = Sumi)
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = soundEnabled,
                onCheckedChange = { onToggleSound() },
                colors = SwitchDefaults.colors(checkedTrackColor = Shu, checkedThumbColor = Kinari),
            )
        }
    }
}

/** 王将の駒と題字。 */
@Composable
private fun Logo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            PieceView(PieceType.KIN, upsideDown = false, height = 70.dp, elevation = 6.dp)
            PieceView(PieceType.OU, upsideDown = false, height = 100.dp, elevation = 10.dp)
            PieceView(PieceType.GIN, upsideDown = false, height = 70.dp, elevation = 6.dp)
        }
        Spacer(Modifier.height(20.dp))
        Text("将棋", fontFamily = Mincho, fontWeight = FontWeight.Black, fontSize = 60.sp, color = Sumi, letterSpacing = 12.sp)
        Box(
            Modifier
                .padding(top = 4.dp)
                .width(120.dp)
                .height(3.dp)
                .background(Shu),
        )
        Text(
            "S H O G I",
            fontFamily = Mincho,
            fontSize = 14.sp,
            color = Sumi.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun LobbyScreen(
    lobby: LobbyState,
    onCreate: () -> Unit,
    onJoin: (String) -> Unit,
    onCancelWaiting: () -> Unit,
    onBack: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .washiBackground()
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("オンライン対戦", fontFamily = Mincho, fontWeight = FontWeight.Black, fontSize = 30.sp, color = Sumi)

        when {
            lobby.waitingCode != null -> {
                Text("部屋番号", fontFamily = Mincho, color = Sumi)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(2.dp, Kincha, RoundedCornerShape(12.dp))
                        .background(Kinari, RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        lobby.waitingCode,
                        fontFamily = Mincho,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 8.sp,
                        color = Shu,
                    )
                }
                Text(
                    "この番号を相手に伝えてください。\n相手が参加すると対局が始まります(あなたは先手)。",
                    textAlign = TextAlign.Center,
                    color = Sumi,
                )
                CircularProgressIndicator(color = Shu)
                SumiButton("やめる", onClick = onCancelWaiting)
            }
            lobby.busy -> CircularProgressIndicator(color = Shu)
            else -> {
                ShuButton("部屋を作る(先手)", Modifier.fillMaxWidth(), height = 56.dp, onClick = onCreate)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Kincha)
                OutlinedTextField(
                    value = code,
                    onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                    label = { Text("部屋番号(6桁)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                ShuButton(
                    "部屋に参加する(後手)",
                    Modifier.fillMaxWidth(),
                    enabled = code.length == 6,
                    height = 56.dp,
                ) { onJoin(code) }
            }
        }

        lobby.error?.let {
            Text(it, color = Shu, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onBack) { Text("メニューに戻る", color = Sumi, fontFamily = Mincho) }
    }
}

package com.tktkcompany.shogi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.tktkcompany.shogi.ui.theme.PromotedText

/** アプリ全体。画面(ホーム / オンラインロビー / 対局)を切り替える。 */
@Composable
fun ShogiApp(viewModel: ShogiViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (viewModel.screen) {
            Screen.HOME -> HomeScreen(
                onlineAvailable = viewModel.onlineAvailable,
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
    onStart: (GameMode) -> Unit,
    onOnline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("将棋", fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        MenuButton(GameMode.VS_CPU.label) { onStart(GameMode.VS_CPU) }
        MenuButton(GameMode.VS_HUMAN.label) { onStart(GameMode.VS_HUMAN) }
        MenuButton(GameMode.ONLINE.label, enabled = onlineAvailable, onClick = onOnline)
        if (!onlineAvailable) {
            Text(
                "オンライン対戦を使うには Firebase の設定(google-services.json)が必要です",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MenuButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(label, fontSize = 18.sp)
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
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("オンライン対戦", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        when {
            lobby.waitingCode != null -> {
                Text("部屋番号", style = MaterialTheme.typography.titleMedium)
                Text(lobby.waitingCode, fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
                Text(
                    "この番号を相手に伝えてください。\n相手が参加すると対局が始まります(あなたは先手)。",
                    textAlign = TextAlign.Center,
                )
                CircularProgressIndicator()
                OutlinedButton(onClick = onCancelWaiting) { Text("やめる") }
            }
            lobby.busy -> CircularProgressIndicator()
            else -> {
                MenuButton("部屋を作る(先手)", onClick = onCreate)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                    label = { Text("部屋番号(6桁)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                MenuButton("部屋に参加する(後手)", enabled = code.length == 6) { onJoin(code) }
            }
        }

        lobby.error?.let {
            Text(it, color = PromotedText, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onBack) { Text("メニューに戻る") }
    }
}

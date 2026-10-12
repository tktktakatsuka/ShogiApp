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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tktkcompany.shogi.R
import com.tktkcompany.shogi.UiText
import com.tktkcompany.shogi.game.CpuLevel
import com.tktkcompany.shogi.game.KifuStyle
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
    // 棋譜の表記は端末の言語に合わせる(日本語なら日本式、それ以外は英語式)
    val kifuStyle = if (booleanResource(R.bool.western_kifu)) KifuStyle.WESTERN else KifuStyle.JAPANESE
    LaunchedEffect(kifuStyle) { viewModel.setKifuStyle(kifuStyle) }
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
                cpuLevels = viewModel.cpuLevels,
                cpuLevel = viewModel.cpuLevel,
                onStartCpu = viewModel::startCpuGame,
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
    onlineProblem: UiText?,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    cpuLevels: List<CpuLevel>,
    cpuLevel: CpuLevel,
    onStartCpu: (CpuLevel) -> Unit,
    onStart: (GameMode) -> Unit,
    onOnline: () -> Unit,
) {
    var choosingLevel by rememberSaveable { mutableStateOf(false) }
    // 強さを選べるとき(エンジンが入っているとき)だけ、強さの選択画面を出す
    val startCpu: (GameMode) -> Unit = { mode ->
        when {
            mode != GameMode.VS_CPU -> onStart(mode)
            cpuLevels.size > 1 -> choosingLevel = true
            else -> onStartCpu(cpuLevels.first())
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .washiBackground()
            .safeDrawingPadding(),
    ) {
        HomeMenu(onlineAvailable, onlineProblem, soundEnabled, onToggleSound, startCpu, onOnline, Modifier.weight(1f))
        AdBanner()
    }
    if (choosingLevel) {
        CpuLevelDialog(
            levels = cpuLevels,
            current = cpuLevel,
            onChoose = { level ->
                choosingLevel = false
                onStartCpu(level)
            },
            onDismiss = { choosingLevel = false },
        )
    }
}

/** CPU の強さを選ぶ画面。前回選んだ強さを朱色で示す。 */
@Composable
private fun CpuLevelDialog(
    levels: List<CpuLevel>,
    current: CpuLevel,
    onChoose: (CpuLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cpu_level_title), fontFamily = Mincho, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (level in levels) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        val label = stringResource(level.label)
                        if (level == current) {
                            ShuButton(label, Modifier.fillMaxWidth()) { onChoose(level) }
                        } else {
                            SumiButton(label, Modifier.fillMaxWidth()) { onChoose(level) }
                        }
                        Text(
                            stringResource(level.description),
                            fontFamily = Mincho,
                            fontSize = 12.sp,
                            color = Sumi.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Sumi) }
        },
    )
}

@Composable
private fun HomeMenu(
    onlineAvailable: Boolean,
    onlineProblem: UiText?,
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
        ShuButton(stringResource(GameMode.VS_CPU.label), Modifier.fillMaxWidth(), height = 56.dp) {
            onStart(GameMode.VS_CPU)
        }
        ShuButton(stringResource(GameMode.VS_HUMAN.label), Modifier.fillMaxWidth(), height = 56.dp) {
            onStart(GameMode.VS_HUMAN)
        }
        ShuButton(
            stringResource(GameMode.ONLINE.label),
            Modifier.fillMaxWidth(),
            enabled = onlineAvailable,
            height = 56.dp,
            onClick = onOnline,
        )
        if (!onlineAvailable) {
            Text(
                onlineProblem?.resolve() ?: stringResource(R.string.online_needs_firebase),
                fontSize = 12.sp,
                color = Shu,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sound_and_vibration), fontFamily = Mincho, color = Sumi)
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
        Text(
            stringResource(R.string.mode_online),
            fontFamily = Mincho,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            color = Sumi,
        )

        when {
            lobby.waitingCode != null -> {
                Text(stringResource(R.string.room_code), fontFamily = Mincho, color = Sumi)
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
                    stringResource(R.string.lobby_waiting),
                    textAlign = TextAlign.Center,
                    color = Sumi,
                )
                InviteButtons(lobby.waitingCode)
                CircularProgressIndicator(color = Shu)
                SumiButton(stringResource(R.string.cancel), onClick = onCancelWaiting)
            }
            lobby.busy -> CircularProgressIndicator(color = Shu)
            else -> {
                ShuButton(
                    stringResource(R.string.create_room),
                    Modifier.fillMaxWidth(),
                    height = 56.dp,
                    onClick = onCreate,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Kincha)
                OutlinedTextField(
                    value = code,
                    onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                    label = { Text(stringResource(R.string.room_code_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                ShuButton(
                    stringResource(R.string.join_room),
                    Modifier.fillMaxWidth(),
                    enabled = code.length == 6,
                    height = 56.dp,
                ) { onJoin(code) }
            }
        }

        lobby.error?.let {
            Text(it.resolve(), color = Shu, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_menu), color = Sumi, fontFamily = Mincho)
        }
    }
}

/** 部屋番号と招待リンクを LINE などで送るボタン。 */
@Composable
private fun InviteButtons(code: String) {
    val context = LocalContext.current
    val message = stringResource(R.string.invite_message, code, Invite.link(code))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ShuButton(stringResource(R.string.invite_line), Modifier.fillMaxWidth()) {
            Invite.sendWithLine(context, message)
        }
        SumiButton(stringResource(R.string.invite_other), Modifier.fillMaxWidth()) {
            Invite.sendWithChooser(context, message)
        }
    }
}

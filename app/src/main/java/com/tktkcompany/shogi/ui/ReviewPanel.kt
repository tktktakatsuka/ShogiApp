package com.tktkcompany.shogi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tktkcompany.shogi.ui.components.ShuButton
import com.tktkcompany.shogi.ui.components.SumiButton
import com.tktkcompany.shogi.ui.theme.AiHint
import com.tktkcompany.shogi.ui.theme.Kincha
import com.tktkcompany.shogi.ui.theme.Kinari
import com.tktkcompany.shogi.ui.theme.Mincho
import com.tktkcompany.shogi.ui.theme.Shu
import com.tktkcompany.shogi.ui.theme.Sumi
import com.tktkcompany.shogi.ui.theme.WashiShade

/** 感想戦の操作ボタン(最初・1手戻る・1手進む・最後・本譜に戻る)と、AIの最善手。 */
@Composable
fun ReviewControls(review: ReviewUi, ai: AiUi?, viewModel: ShogiViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (ai != null) AiResult(ai)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            val canBack = review.index > 0 || review.isVariation
            val canForward = !review.isVariation && review.index < review.total
            SumiButton("⏮", Modifier.weight(1f), enabled = canBack && !review.isVariation, onClick = viewModel::reviewFirst)
            SumiButton("◀", Modifier.weight(1f), enabled = canBack, onClick = viewModel::reviewPrev)
            SumiButton("▶", Modifier.weight(1f), enabled = canForward, onClick = viewModel::reviewNext)
            SumiButton("⏭", Modifier.weight(1f), enabled = canForward, onClick = viewModel::reviewLast)
            if (viewModel.aiAvailable) AiToggle(on = ai != null, Modifier.weight(1f), onClick = viewModel::toggleAi)
        }
        if (review.isVariation) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "変化　" + review.variationNotations.joinToString(" "),
                    fontFamily = Mincho,
                    fontSize = 13.sp,
                    color = Shu,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::reviewBackToMainLine) {
                    Text("本譜に戻る", fontFamily = Mincho, color = Sumi, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** AIの最善手の表示を切り替えるボタン。 */
@Composable
private fun AiToggle(on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .background(if (on) AiHint else WashiShade.copy(alpha = 0f), RoundedCornerShape(10.dp))
            .border(1.5.dp, AiHint, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            "AI",
            fontFamily = Mincho,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (on) Kinari else AiHint,
        )
    }
}

/** AIの最善手・形勢・読み筋。 */
@Composable
private fun AiResult(ai: AiUi) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(46.dp),
    ) {
        Text("AI", fontFamily = Mincho, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AiHint)
        Column(Modifier.weight(1f)) {
            val best = ai.line.firstOrNull()
            Text(
                when {
                    ai.note != null -> ai.note
                    best == null -> "考えています…"
                    else -> "$best　${ai.score}" + if (ai.thinking) " …" else ""
                },
                fontFamily = Mincho,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Sumi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (ai.line.size > 1) {
                Text(
                    "読み筋　" + ai.line.drop(1).joinToString(" "),
                    fontFamily = Mincho,
                    fontSize = 12.sp,
                    color = Sumi.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 感想戦の下側のパネル。オンラインでは「チャット」と「棋譜」を切り替えられる。
 */
@Composable
fun ReviewBottomPanel(state: UiState, viewModel: ShogiViewModel, modifier: Modifier = Modifier) {
    val review = state.review ?: return
    val chatEnabled = state.online != null
    var showChat by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Kincha, RoundedCornerShape(10.dp))
            .background(Kinari.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(6.dp),
    ) {
        if (chatEnabled) {
            Row {
                PanelTab("チャット", selected = showChat) { showChat = true }
                PanelTab("棋譜", selected = !showChat) { showChat = false }
            }
        }
        if (chatEnabled && showChat) {
            ChatPanel(state.chat, onSend = viewModel::sendChat, modifier = Modifier.weight(1f))
        } else {
            KifuList(review, onJump = viewModel::reviewJumpTo, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PanelTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 6.dp, bottom = 4.dp)
            .background(if (selected) Shu else WashiShade, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            fontFamily = Mincho,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selected) Kinari else Sumi,
        )
    }
}

/** 本譜の棋譜一覧。タップでその局面へ移動する。 */
@Composable
private fun KifuList(review: ReviewUi, onJump: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(review.index) {
        listState.animateScrollToItem((review.index - 2).coerceAtLeast(0))
    }
    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        item {
            KifuRow(label = "開始局面", current = review.index == 0 && !review.isVariation) { onJump(0) }
        }
        itemsIndexed(review.mainNotations) { i, notation ->
            KifuRow(
                label = "${i + 1}　$notation",
                current = review.index == i + 1 && !review.isVariation,
            ) { onJump(i + 1) }
        }
    }
}

@Composable
private fun KifuRow(label: String, current: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontFamily = Mincho,
        fontSize = 15.sp,
        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
        color = if (current) Kinari else Sumi,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (current) Shu else Kinari.copy(alpha = 0f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private val QUICK_PHRASES = listOf(
    "ありがとうございました",
    "参りました",
    "ここが悪かった？",
    "この手は気づかなかった",
    "なるほど！",
    "もう一局どう？",
)

/** チャット欄(定型文ボタン付き)。 */
@Composable
private fun ChatPanel(messages: List<ChatLine>, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "盤面を動かしながら\n相手と話してみましょう",
                    fontFamily = Mincho,
                    color = Sumi.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(messages, key = { it.id }) { ChatBubble(it) }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            items(QUICK_PHRASES) { phrase ->
                Text(
                    phrase,
                    fontFamily = Mincho,
                    fontSize = 13.sp,
                    color = Sumi,
                    modifier = Modifier
                        .border(1.dp, Kincha, RoundedCornerShape(14.dp))
                        .clickable { onSend(phrase) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val send: () -> Unit = {
                onSend(input)
                input = ""
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(200) },
                placeholder = { Text("メッセージ", fontFamily = Mincho) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Shu, unfocusedBorderColor = Kincha),
                modifier = Modifier.weight(1f).height(52.dp),
            )
            ShuButton("送信", enabled = input.isNotBlank(), onClick = send)
        }
    }
}

@Composable
private fun ChatBubble(line: ChatLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (line.mine) Alignment.End else Alignment.Start) {
            Text(
                if (line.mine) "あなた" else "相手",
                fontFamily = Mincho,
                fontSize = 11.sp,
                color = Sumi.copy(alpha = 0.6f),
            )
            Text(
                line.text,
                fontFamily = Mincho,
                fontSize = 15.sp,
                color = if (line.mine) Kinari else Sumi,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .background(if (line.mine) Shu else WashiShade, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

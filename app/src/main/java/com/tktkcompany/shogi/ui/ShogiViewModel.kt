package com.tktkcompany.shogi.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tktkcompany.shogi.game.Cpu
import com.tktkcompany.shogi.game.Game
import com.tktkcompany.shogi.game.GameResult
import com.tktkcompany.shogi.game.Move
import com.tktkcompany.shogi.game.PieceType
import com.tktkcompany.shogi.game.Player
import com.tktkcompany.shogi.game.Position
import com.tktkcompany.shogi.game.Review
import com.tktkcompany.shogi.game.ReviewState
import com.tktkcompany.shogi.game.Square
import com.tktkcompany.shogi.game.Usi
import com.tktkcompany.shogi.online.FirebaseOnlineRepository
import com.tktkcompany.shogi.online.OnlineRepository
import com.tktkcompany.shogi.online.RemoteMove
import com.tktkcompany.shogi.online.RemoteReview
import com.tktkcompany.shogi.online.Room
import com.tktkcompany.shogi.online.RoomStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameMode(val label: String) {
    VS_CPU("CPUと対局"),
    VS_HUMAN("2人で対局"),
    ONLINE("オンライン対戦"),
}

enum class Screen { HOME, LOBBY, GAME }

/** 盤上の駒か持ち駒のどちらを選択中か。 */
sealed interface Selection {
    data class OnBoard(val square: Square) : Selection
    data class InHand(val type: PieceType) : Selection
}

/** オンライン対局中の情報。 */
data class OnlineSession(val code: String, val uid: String, val myColor: Player)

data class LobbyState(
    val busy: Boolean = false,
    val waitingCode: String? = null,
    val error: String? = null,
)

/** 感想戦の表示内容。 */
data class ReviewUi(
    /** 本譜の何手目の局面から見ているか。 */
    val index: Int,
    /** 本譜の手数。 */
    val total: Int,
    val isVariation: Boolean,
    val variationNotations: List<String>,
    val mainNotations: List<String>,
)

/** チャットの1行。 */
data class ChatLine(val id: String, val text: String, val mine: Boolean)

data class UiState(
    val position: Position,
    val mode: GameMode,
    /** 盤の手前側に表示するプレイヤー。 */
    val viewer: Player = Player.SENTE,
    val selection: Selection? = null,
    val targets: Set<Square> = emptySet(),
    val lastMove: Move? = null,
    val lastMoveNotation: String? = null,
    val pendingPromotion: Pair<Move, Move>? = null,
    val result: GameResult? = null,
    val showResultDialog: Boolean = false,
    val cpuThinking: Boolean = false,
    val moveCount: Int = 0,
    val online: OnlineSession? = null,
    val error: String? = null,
    /** 感想戦中なら non-null。 */
    val review: ReviewUi? = null,
    val chat: List<ChatLine> = emptyList(),
    /** オンラインで相手が感想戦を始めたが、自分はまだ入っていない。 */
    val partnerInReview: Boolean = false,
) {
    val inCheck: Boolean get() = position.isInCheck(position.turn)

    /** 駒台に表示するプレイヤー名。 */
    fun nameOf(player: Player): String = when (mode) {
        GameMode.VS_HUMAN -> player.label
        GameMode.VS_CPU -> if (player == Player.SENTE) "あなた" else "CPU"
        GameMode.ONLINE -> if (player == online?.myColor) "あなた" else "相手"
    }
}

class ShogiViewModel(application: Application) : AndroidViewModel(application) {
    private var game = Game()
    private val cpu = Cpu()
    private var cpuJob: Job? = null

    /** CPU は後手を担当する。 */
    private val cpuPlayer = Player.GOTE

    private val onlineSetup = FirebaseOnlineRepository.create(application)
    private val repository: OnlineRepository? = onlineSetup.repository
    private var roomJob: Job? = null
    private var online: OnlineSession? = null
    private var resultDismissed = false
    private var latestRoom: Room? = null

    /** 感想戦中なら non-null。 */
    private var review: Review? = null
    private var reviewState = ReviewState(0)

    /** Firebase が設定されていればオンライン対戦を使える。 */
    val onlineAvailable: Boolean get() = repository != null

    /** オンライン対戦が使えない理由。 */
    val onlineProblem: String? get() = onlineSetup.problem

    var screen by mutableStateOf(Screen.HOME)
        private set

    var lobby by mutableStateOf(LobbyState())
        private set

    var state by mutableStateOf(UiState(game.position, GameMode.VS_CPU))
        private set

    private val prefs = application.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)

    /** 効果音・振動のオン/オフ(端末に保存する)。 */
    var soundEnabled by mutableStateOf(prefs.getBoolean(KEY_SOUND, true))
        private set

    fun toggleSound() {
        soundEnabled = !soundEnabled
        prefs.edit().putBoolean(KEY_SOUND, soundEnabled).apply()
    }

    // ---- 画面遷移 ----

    fun startLocalGame(mode: GameMode) {
        stopOnline()
        cpuJob?.cancel()
        game = Game()
        review = null
        resultDismissed = false
        state = UiState(game.position, mode)
        screen = Screen.GAME
    }

    fun openLobby() {
        stopOnline()
        lobby = LobbyState()
        screen = Screen.LOBBY
    }

    /** ホームへ戻る。オンライン対局中なら投了扱いにする。 */
    fun backToHome() {
        cpuJob?.cancel()
        if (state.mode == GameMode.ONLINE && screen == Screen.GAME && !game.isOver) resign()
        stopOnline()
        screen = Screen.HOME
    }

    // ---- オンライン: 部屋 ----

    fun createRoom() {
        val repo = repository ?: return
        lobby = LobbyState(busy = true)
        viewModelScope.launch {
            try {
                val uid = repo.signIn()
                val code = repo.createRoom(uid)
                lobby = LobbyState(waitingCode = code)
                observeRoom(repo, code, uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lobby = LobbyState(error = e.message ?: "部屋を作れませんでした")
            }
        }
    }

    fun joinRoom(code: String) {
        val repo = repository ?: return
        val trimmed = code.trim()
        if (trimmed.length != 6 || !trimmed.all { it.isDigit() }) {
            lobby = LobbyState(error = "部屋番号は6桁の数字です")
            return
        }
        lobby = LobbyState(busy = true)
        viewModelScope.launch {
            try {
                val uid = repo.signIn()
                repo.joinRoom(trimmed, uid)
                observeRoom(repo, trimmed, uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lobby = LobbyState(error = e.message ?: "部屋に参加できませんでした")
            }
        }
    }

    fun cancelWaiting() {
        stopOnline()
        lobby = LobbyState()
    }

    private fun stopOnline() {
        roomJob?.cancel()
        roomJob = null
        online = null
        latestRoom = null
        review = null
    }

    private fun observeRoom(repo: OnlineRepository, code: String, uid: String) {
        roomJob?.cancel()
        roomJob = viewModelScope.launch {
            try {
                repo.observeRoom(code).collect { room -> if (room != null) onRoomUpdated(room, uid) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "通信エラーが発生しました"
                if (screen == Screen.GAME) state = state.copy(error = message)
                else lobby = LobbyState(error = message)
            }
        }
    }

    private fun onRoomUpdated(room: Room, uid: String) {
        val myColor = room.colorOf(uid) ?: return
        if (room.status == RoomStatus.WAITING) return
        val replay = room.replay(current = if (online == null) null else game)
        val firstUpdate = online == null
        online = OnlineSession(room.code, uid, myColor)
        latestRoom = room
        game = replay.game
        // 相手が動かした感想戦の局面に合わせる
        val activeReview = review
        val remote = room.review
        if (activeReview != null && remote != null) {
            reviewState = activeReview.normalize(remote.toState())
        }
        if (firstUpdate) {
            resultDismissed = false
            state = UiState(game.position, GameMode.ONLINE, viewer = myColor, online = online)
            screen = Screen.GAME
        }
        refresh()
        if (replay.invalidAt != null) {
            state = state.copy(error = "${replay.invalidAt + 1}手目に不正な手を受信したため、その手を無視しました")
        }
    }

    // ---- 感想戦 ----

    /** 終局後に感想戦を始める。オンラインでは相手と局面を共有する。 */
    fun startReview() {
        if (!game.isOver || review != null) return
        val r = Review(game.moves)
        review = r
        resultDismissed = true
        val remote = latestRoom?.review
        reviewState = if (remote != null) r.normalize(remote.toState()) else r.end()
        refresh()
        // 相手に感想戦を始めたことを知らせる(相手がすでに始めていれば、その局面に合わせるだけ)
        if (remote == null) shareReview()
    }

    fun exitReview() {
        review = null
        refresh()
    }

    fun reviewFirst() = updateReview { it.jumpTo(0) }
    fun reviewPrev() = updateReview { r -> r.back(reviewState) }
    fun reviewNext() = updateReview { r -> r.forward(reviewState) }
    fun reviewLast() = updateReview { it.end() }
    fun reviewJumpTo(index: Int) = updateReview { it.jumpTo(index) }
    fun reviewBackToMainLine() = updateReview { r -> r.backToMainLine(reviewState) }

    private fun updateReview(change: (Review) -> ReviewState) {
        val r = review ?: return
        val next = change(r)
        if (next == reviewState) return
        reviewState = next
        refresh()
        shareReview()
    }

    private fun shareReview() {
        val session = online ?: return
        val repo = repository ?: return
        val shared = RemoteReview.from(reviewState)
        viewModelScope.launch {
            try {
                repo.sendReview(session.code, session.uid, shared)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "感想戦の局面を共有できませんでした")
            }
        }
    }

    /** チャットを送る(オンラインのみ)。 */
    fun sendChat(text: String) {
        val session = online ?: return
        val repo = repository ?: return
        val message = text.trim().take(200)
        if (message.isEmpty()) return
        viewModelScope.launch {
            try {
                repo.sendChat(session.code, session.uid, message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "メッセージを送れませんでした")
            }
        }
    }

    /** 盤に表示している局面(感想戦中は感想戦の局面)。 */
    private fun displayPosition(): Position = review?.positionOf(reviewState) ?: game.position

    // ---- 対局操作 ----

    fun onSquareTapped(square: Square) {
        if (!canHumanMove()) return
        val position = displayPosition()
        val selection = state.selection
        if (selection != null && square in state.targets) {
            val candidates = when (selection) {
                is Selection.OnBoard -> position.legalMovesFrom(selection.square)
                is Selection.InHand -> position.legalDrops(selection.type)
            }.filter { it.to == square }
            when (candidates.size) {
                1 -> play(candidates.single())
                2 -> state = state.copy(
                    pendingPromotion = candidates.first { it.promote } to candidates.first { !it.promote }
                )
            }
            return
        }
        val piece = position[square]
        state = if (piece != null && piece.owner == position.turn && selection != Selection.OnBoard(square)) {
            state.copy(
                selection = Selection.OnBoard(square),
                targets = position.legalMovesFrom(square).map { it.to }.toSet(),
            )
        } else {
            state.copy(selection = null, targets = emptySet())
        }
    }

    fun onHandPieceTapped(owner: Player, type: PieceType) {
        val position = displayPosition()
        if (!canHumanMove() || owner != position.turn) return
        val sel = Selection.InHand(type)
        state = if (state.selection == sel) {
            state.copy(selection = null, targets = emptySet())
        } else {
            state.copy(
                selection = sel,
                targets = position.legalDrops(type).map { it.to }.toSet(),
            )
        }
    }

    fun onPromotionChosen(promote: Boolean) {
        val (yes, no) = state.pendingPromotion ?: return
        play(if (promote) yes else no)
    }

    fun onPromotionCancelled() {
        state = state.copy(pendingPromotion = null)
    }

    fun undo() {
        when (state.mode) {
            GameMode.ONLINE -> return
            GameMode.VS_CPU -> {
                cpuJob?.cancel()
                // CPU戦では自分の手番まで戻す
                game.undo()
                if (game.position.turn == cpuPlayer) game.undo()
            }
            GameMode.VS_HUMAN -> game.undo()
        }
        refresh()
    }

    /** 投了する。2人対局では手番側、CPU戦・オンラインでは自分が投了する。 */
    fun resign() {
        if (game.isOver) return
        cpuJob?.cancel()
        when (state.mode) {
            GameMode.VS_HUMAN -> game.resign(game.position.turn)
            GameMode.VS_CPU -> game.resign(cpuPlayer.opponent)
            GameMode.ONLINE -> {
                val session = online ?: return
                val repo = repository ?: return
                game.resign(session.myColor)
                viewModelScope.launch {
                    try {
                        repo.resign(session.code, session.uid)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        state = state.copy(error = "投了の送信に失敗しました")
                    }
                }
            }
        }
        refresh()
    }

    fun dismissResult() {
        resultDismissed = true
        state = state.copy(showResultDialog = false)
    }

    fun dismissError() {
        state = state.copy(error = null)
    }

    fun canUndo(): Boolean = state.mode != GameMode.ONLINE && review == null && state.moveCount > 0

    private fun canHumanMove(): Boolean {
        // 感想戦ではどちらの駒も動かせる
        if (review != null) return true
        if (game.isOver || state.cpuThinking) return false
        val turn = game.position.turn
        return when (state.mode) {
            GameMode.VS_HUMAN -> true
            GameMode.VS_CPU -> turn != cpuPlayer
            GameMode.ONLINE -> online?.myColor == turn
        }
    }

    private fun play(move: Move) {
        val r = review
        if (r != null) {
            val next = r.play(reviewState, move) ?: return
            reviewState = next
            refresh()
            shareReview()
            return
        }
        if (!game.play(move)) return
        refresh()
        when (state.mode) {
            GameMode.VS_CPU -> if (!game.isOver && game.position.turn == cpuPlayer) startCpuTurn()
            GameMode.ONLINE -> sendMove(move, index = game.moveCount - 1)
            GameMode.VS_HUMAN -> Unit
        }
    }

    private fun sendMove(move: Move, index: Int) {
        val session = online ?: return
        val repo = repository ?: return
        viewModelScope.launch {
            try {
                repo.sendMove(session.code, index, RemoteMove(Usi.format(move), session.uid))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(error = "指し手の送信に失敗しました。通信状態を確認してください")
            }
        }
    }

    private fun startCpuTurn() {
        state = state.copy(cpuThinking = true)
        cpuJob = viewModelScope.launch {
            val position = game.position
            delay(300)
            val move = withContext(Dispatchers.Default) { cpu.chooseMove(position) }
            if (move != null) game.play(move)
            refresh()
        }
    }

    private fun refresh() {
        if (game.result == null) resultDismissed = false
        val r = review
        val view = r?.view(reviewState)
        val position = view?.position ?: game.position
        // 局面が変わらない更新(チャットの受信など)では、駒の選択を解除しない
        val samePosition = position === state.position || position.toSfen() == state.position.toSfen()
        val myUid = online?.uid
        state = state.copy(
            position = position,
            selection = if (samePosition) state.selection else null,
            targets = if (samePosition) state.targets else emptySet(),
            lastMove = if (view != null) view.lastMove else game.lastMove,
            lastMoveNotation = if (view != null) view.lastMoveNotation else game.lastMoveNotation,
            pendingPromotion = if (samePosition) state.pendingPromotion else null,
            result = game.result,
            showResultDialog = game.result != null && !resultDismissed,
            cpuThinking = false,
            moveCount = view?.ply ?: game.moveCount,
            online = online,
            review = if (r != null && view != null) {
                ReviewUi(
                    index = reviewState.index.coerceIn(0, r.size),
                    total = r.size,
                    isVariation = view.variationNotations.isNotEmpty(),
                    variationNotations = view.variationNotations,
                    mainNotations = r.mainNotations,
                )
            } else {
                null
            },
            chat = latestRoom?.chat.orEmpty().map { ChatLine(it.id, it.text, mine = it.by == myUid) },
            partnerInReview = r == null && game.isOver &&
                latestRoom?.review?.by.let { it != null && it != myUid },
        )
    }

    private companion object {
        const val KEY_SOUND = "sound"
    }
}

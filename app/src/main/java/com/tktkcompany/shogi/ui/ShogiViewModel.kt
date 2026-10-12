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
import com.tktkcompany.shogi.game.Square
import com.tktkcompany.shogi.game.Usi
import com.tktkcompany.shogi.online.FirebaseOnlineRepository
import com.tktkcompany.shogi.online.OnlineRepository
import com.tktkcompany.shogi.online.RemoteMove
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

data class UiState(
    val position: Position,
    val mode: GameMode,
    /** 盤の手前側に表示するプレイヤー。 */
    val viewer: Player = Player.SENTE,
    val selection: Selection? = null,
    val targets: Set<Square> = emptySet(),
    val lastMove: Move? = null,
    val pendingPromotion: Pair<Move, Move>? = null,
    val result: GameResult? = null,
    val showResultDialog: Boolean = false,
    val cpuThinking: Boolean = false,
    val moveCount: Int = 0,
    val online: OnlineSession? = null,
    val error: String? = null,
) {
    val inCheck: Boolean get() = position.isInCheck(position.turn)
}

class ShogiViewModel(application: Application) : AndroidViewModel(application) {
    private var game = Game()
    private val cpu = Cpu()
    private var cpuJob: Job? = null

    /** CPU は後手を担当する。 */
    private val cpuPlayer = Player.GOTE

    private val repository: OnlineRepository? = FirebaseOnlineRepository.createOrNull(application)
    private var roomJob: Job? = null
    private var online: OnlineSession? = null
    private var resultDismissed = false

    /** Firebase が設定されていればオンライン対戦を使える。 */
    val onlineAvailable: Boolean get() = repository != null

    var screen by mutableStateOf(Screen.HOME)
        private set

    var lobby by mutableStateOf(LobbyState())
        private set

    var state by mutableStateOf(UiState(game.position, GameMode.VS_CPU))
        private set

    // ---- 画面遷移 ----

    fun startLocalGame(mode: GameMode) {
        stopOnline()
        cpuJob?.cancel()
        game = Game()
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
        game = replay.game
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

    // ---- 対局操作 ----

    fun onSquareTapped(square: Square) {
        if (!canHumanMove()) return
        val position = game.position
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
        if (!canHumanMove() || owner != game.position.turn) return
        val sel = Selection.InHand(type)
        state = if (state.selection == sel) {
            state.copy(selection = null, targets = emptySet())
        } else {
            state.copy(
                selection = sel,
                targets = game.position.legalDrops(type).map { it.to }.toSet(),
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

    fun canUndo(): Boolean = state.mode != GameMode.ONLINE && state.moveCount > 0

    private fun canHumanMove(): Boolean {
        if (game.isOver || state.cpuThinking) return false
        val turn = game.position.turn
        return when (state.mode) {
            GameMode.VS_HUMAN -> true
            GameMode.VS_CPU -> turn != cpuPlayer
            GameMode.ONLINE -> online?.myColor == turn
        }
    }

    private fun play(move: Move) {
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
        state = state.copy(
            position = game.position,
            selection = null,
            targets = emptySet(),
            lastMove = game.lastMove,
            pendingPromotion = null,
            result = game.result,
            showResultDialog = game.result != null && !resultDismissed,
            cpuThinking = false,
            moveCount = game.moveCount,
            online = online,
        )
    }
}

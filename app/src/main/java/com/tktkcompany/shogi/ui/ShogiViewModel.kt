package com.tktkcompany.shogi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tktkcompany.shogi.game.Cpu
import com.tktkcompany.shogi.game.Game
import com.tktkcompany.shogi.game.Move
import com.tktkcompany.shogi.game.PieceType
import com.tktkcompany.shogi.game.Player
import com.tktkcompany.shogi.game.Position
import com.tktkcompany.shogi.game.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameMode(val label: String) {
    VS_HUMAN("2人で対局"),
    VS_CPU("CPUと対局"),
}

/** 盤上の駒か持ち駒のどちらを選択中か。 */
sealed interface Selection {
    data class OnBoard(val square: Square) : Selection
    data class InHand(val type: PieceType) : Selection
}

data class UiState(
    val position: Position,
    val mode: GameMode,
    val selection: Selection? = null,
    val targets: Set<Square> = emptySet(),
    val lastMove: Move? = null,
    val pendingPromotion: Pair<Move, Move>? = null,
    val resultMessage: String? = null,
    val cpuThinking: Boolean = false,
    val moveCount: Int = 0,
) {
    val inCheck: Boolean get() = position.isInCheck(position.turn)
}

class ShogiViewModel : ViewModel() {
    private var game = Game()
    private val cpu = Cpu()
    private var cpuJob: Job? = null

    /** CPU は後手を担当する。 */
    private val cpuPlayer = Player.GOTE

    var state by mutableStateOf(UiState(game.position, GameMode.VS_CPU))
        private set

    fun newGame(mode: GameMode = state.mode) {
        cpuJob?.cancel()
        game = Game()
        state = UiState(game.position, mode)
    }

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
        cpuJob?.cancel()
        if (state.mode == GameMode.VS_CPU) {
            // CPU戦では自分の手番まで戻す
            game.undo()
            if (game.position.turn == cpuPlayer) game.undo()
        } else {
            game.undo()
        }
        refresh()
    }

    fun dismissResult() {
        state = state.copy(resultMessage = null)
    }

    private fun canHumanMove(): Boolean =
        !game.isOver && !state.cpuThinking &&
            !(state.mode == GameMode.VS_CPU && game.position.turn == cpuPlayer)

    private fun play(move: Move) {
        game.play(move)
        refresh()
        if (state.mode == GameMode.VS_CPU && !game.isOver && game.position.turn == cpuPlayer) {
            startCpuTurn()
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
        state = UiState(
            position = game.position,
            mode = state.mode,
            lastMove = game.lastMove,
            resultMessage = game.result?.message,
            moveCount = game.moveCount,
        )
    }
}

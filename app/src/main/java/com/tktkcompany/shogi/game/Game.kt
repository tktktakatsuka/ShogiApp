package com.tktkcompany.shogi.game

enum class EndReason {
    CHECKMATE,
    RESIGN,
    SENNICHITE,
}

/** 終局結果。winner が null なら引き分け。 */
data class GameResult(val winner: Player?, val reason: EndReason) {
    val message: String
        get() = when (reason) {
            EndReason.CHECKMATE -> "詰み。${winner?.label}の勝ち"
            EndReason.RESIGN -> "${winner?.opponent?.label}の投了。${winner?.label}の勝ち"
            EndReason.SENNICHITE -> "千日手(引き分け)"
        }
}

/** 対局全体の進行(局面履歴・待った・終局判定)を管理する。 */
class Game(initial: Position = Position.initial()) {
    private val positions = mutableListOf(initial)
    private val moveList = mutableListOf<Move>()

    val position: Position get() = positions.last()
    val moves: List<Move> get() = moveList
    val moveCount: Int get() = moveList.size
    val lastMove: Move? get() = moveList.lastOrNull()

    var result: GameResult? = null
        private set

    val isOver: Boolean get() = result != null

    fun play(move: Move): Boolean {
        if (isOver || !position.isLegal(move)) return false
        val next = position.apply(move)
        positions += next
        moveList += move
        result = judge(next)
        return true
    }

    /** player が投了する。 */
    fun resign(player: Player) {
        if (!isOver) result = GameResult(player.opponent, EndReason.RESIGN)
    }

    /** 一手戻す。戻せた場合 true。 */
    fun undo(): Boolean {
        if (moveList.isEmpty()) return false
        positions.removeAt(positions.lastIndex)
        moveList.removeAt(moveList.lastIndex)
        result = null
        return true
    }

    private fun judge(next: Position): GameResult? {
        if (next.isCheckmate()) return GameResult(next.turn.opponent, EndReason.CHECKMATE)
        // 同一局面が4回現れたら千日手(連続王手の千日手は簡易化のため区別しない)
        val sfen = next.toSfen()
        if (positions.count { it.toSfen() == sfen } >= 4) return GameResult(null, EndReason.SENNICHITE)
        return null
    }
}

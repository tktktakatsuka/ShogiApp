package com.tktkcompany.shogi.game

/**
 * 感想戦で見ている場所。本譜の [index] 手目まで進めた局面から、[variation] の手を試している状態。
 * variation が空なら本譜の局面そのもの。
 */
data class ReviewState(val index: Int, val variation: List<Move> = emptyList()) {
    val isVariation: Boolean get() = variation.isNotEmpty()
}

/** 感想戦の画面に表示する内容。 */
data class ReviewView(
    val position: Position,
    val lastMove: Move?,
    val lastMoveNotation: String?,
    /** 初手から数えた手数(変化手順を含む)。 */
    val ply: Int,
    /** 試している変化手順の棋譜表記。 */
    val variationNotations: List<String>,
)

/**
 * 終わった対局の感想戦。本譜をたどったり、別の手(変化)を試したりする。
 * 状態は [ReviewState] として外に持たせるので、オンラインでも同じ状態を共有できる。
 */
class Review(
    val mainMoves: List<Move>,
    initial: Position = Position.initial(),
    private val style: KifuStyle = KifuStyle.JAPANESE,
) {
    private val mainPositions: List<Position> = mainMoves.runningFold(initial) { pos, move -> pos.apply(move) }

    /** 本譜の各手の棋譜表記。 */
    val mainNotations: List<String> = mainMoves.mapIndexed { i, move ->
        Kifu.format(mainPositions[i], move, mainMoves.getOrNull(i - 1), style)
    }

    val size: Int get() = mainMoves.size

    /** 最終局面(終局時の状態)。 */
    fun end(): ReviewState = ReviewState(size)

    fun jumpTo(index: Int): ReviewState = ReviewState(index.coerceIn(0, size))

    fun forward(state: ReviewState): ReviewState =
        if (state.isVariation) state else jumpTo(state.index + 1)

    /** 1手戻す。変化を試している間は変化の手を1手取り消す。 */
    fun back(state: ReviewState): ReviewState =
        if (state.isVariation) state.copy(variation = state.variation.dropLast(1)) else jumpTo(state.index - 1)

    /** 変化をやめて本譜に戻る。 */
    fun backToMainLine(state: ReviewState): ReviewState = ReviewState(state.index)

    /** 盤上で手を指す。本譜と同じ手なら本譜を進め、違えば変化として追加する。反則なら null。 */
    fun play(state: ReviewState, move: Move): ReviewState? {
        val s = normalize(state)
        if (!positionOf(s).isLegal(move)) return null
        if (!s.isVariation && mainMoves.getOrNull(s.index) == move) return ReviewState(s.index + 1)
        return s.copy(variation = s.variation + move)
    }

    /** 範囲外の手数や、反則になっている変化の手を取り除く(オンラインで受け取った状態の検証用)。 */
    fun normalize(state: ReviewState): ReviewState {
        val index = state.index.coerceIn(0, size)
        var pos = mainPositions[index]
        val valid = mutableListOf<Move>()
        for (move in state.variation) {
            if (!pos.isLegal(move)) break
            valid += move
            pos = pos.apply(move)
        }
        return ReviewState(index, valid)
    }

    fun positionOf(state: ReviewState): Position =
        state.variation.fold(mainPositions[state.index.coerceIn(0, size)]) { pos, move -> pos.apply(move) }

    fun view(state: ReviewState): ReviewView {
        val s = normalize(state)
        var pos = mainPositions[s.index]
        var previous = mainMoves.getOrNull(s.index - 1)
        val notations = mutableListOf<String>()
        for (move in s.variation) {
            notations += Kifu.format(pos, move, previous, style)
            pos = pos.apply(move)
            previous = move
        }
        return ReviewView(
            position = pos,
            lastMove = previous,
            lastMoveNotation = notations.lastOrNull() ?: mainNotations.getOrNull(s.index - 1),
            ply = s.index + s.variation.size,
            variationNotations = notations,
        )
    }
}

package com.tktkcompany.shogi.game

/**
 * 局面(不変)。盤面・持ち駒・手番を保持し、合法手の生成と着手を行う。
 */
class Position private constructor(
    private val board: Array<Piece?>,
    private val hands: Map<Player, Map<PieceType, Int>>,
    val turn: Player,
) {
    operator fun get(square: Square): Piece? = board[square.index]

    fun handOf(player: Player): Map<PieceType, Int> = hands.getValue(player)

    fun handCount(player: Player, type: PieceType): Int = handOf(player)[type] ?: 0

    fun kingSquare(player: Player): Square? =
        Square.ALL.firstOrNull { board[it.index] == Piece(PieceType.OU, player) }

    // ---- 着手 ----

    /** 指し手を適用した新しい局面を返す(合法性は検査しない)。 */
    fun apply(move: Move): Position {
        val newBoard = board.copyOf()
        val newHands = hands.mapValues { it.value.toMutableMap() }
        val myHand = newHands.getValue(turn)
        if (move.from == null) {
            val type = requireNotNull(move.dropType)
            val count = myHand[type] ?: 0
            require(count > 0) { "持ち駒に${type.kanji}がありません" }
            if (count == 1) myHand.remove(type) else myHand[type] = count - 1
            newBoard[move.to.index] = Piece(type, turn)
        } else {
            val piece = requireNotNull(board[move.from.index]) { "${move.from} に駒がありません" }
            board[move.to.index]?.let { captured ->
                val t = captured.type.unpromoted
                myHand[t] = (myHand[t] ?: 0) + 1
            }
            newBoard[move.from.index] = null
            val type = if (move.promote) requireNotNull(piece.type.promoted) else piece.type
            newBoard[move.to.index] = Piece(type, turn)
        }
        return Position(newBoard, newHands, turn.opponent)
    }

    // ---- 利き ----

    /** 指定の駒が from から動けるマス(自玉の安全は考慮しない)。 */
    private fun pseudoTargets(from: Square, piece: Piece): List<Square> {
        val f = piece.owner.forward
        val result = mutableListOf<Square>()
        fun step(dr: Int, dc: Int) {
            val to = from.offset(dr, dc)
            if (to.isOnBoard && board[to.index]?.owner != piece.owner) result += to
        }
        fun slide(dr: Int, dc: Int) {
            var to = from.offset(dr, dc)
            while (to.isOnBoard) {
                val occupant = board[to.index]
                if (occupant == null) {
                    result += to
                } else {
                    if (occupant.owner != piece.owner) result += to
                    break
                }
                to = to.offset(dr, dc)
            }
        }
        val goldSteps = listOf(f to -1, f to 0, f to 1, 0 to -1, 0 to 1, -f to 0)
        when (piece.type) {
            PieceType.FU -> step(f, 0)
            PieceType.KYO -> slide(f, 0)
            PieceType.KEI -> { step(2 * f, -1); step(2 * f, 1) }
            PieceType.GIN -> listOf(f to -1, f to 0, f to 1, -f to -1, -f to 1).forEach { (r, c) -> step(r, c) }
            PieceType.KIN, PieceType.TO, PieceType.NARIKYO, PieceType.NARIKEI, PieceType.NARIGIN ->
                goldSteps.forEach { (r, c) -> step(r, c) }
            PieceType.OU -> KING_DIRS.forEach { (r, c) -> step(r, c) }
            PieceType.KAKU -> DIAGONALS.forEach { (r, c) -> slide(r, c) }
            PieceType.HI -> ORTHOGONALS.forEach { (r, c) -> slide(r, c) }
            PieceType.UMA -> {
                DIAGONALS.forEach { (r, c) -> slide(r, c) }
                ORTHOGONALS.forEach { (r, c) -> step(r, c) }
            }
            PieceType.RYU -> {
                ORTHOGONALS.forEach { (r, c) -> slide(r, c) }
                DIAGONALS.forEach { (r, c) -> step(r, c) }
            }
        }
        return result
    }

    /** by の駒が target に利いているか。 */
    fun isAttacked(target: Square, by: Player): Boolean =
        Square.ALL.any { sq ->
            val p = board[sq.index]
            p != null && p.owner == by && target in pseudoTargets(sq, p)
        }

    fun isInCheck(player: Player): Boolean {
        val king = kingSquare(player) ?: return false
        return isAttacked(king, player.opponent)
    }

    // ---- 合法手 ----

    /** 手番側の全合法手。 */
    fun legalMoves(): List<Move> =
        Square.ALL.flatMap { legalMovesFrom(it) } +
            handOf(turn).keys.flatMap { legalDrops(it) }

    /** from の駒(手番側)の合法手。成/不成の両方を含む。 */
    fun legalMovesFrom(from: Square): List<Move> {
        val piece = board[from.index] ?: return emptyList()
        if (piece.owner != turn) return emptyList()
        val moves = mutableListOf<Move>()
        for (to in pseudoTargets(from, piece)) {
            if (canPromote(piece, from, to)) moves += Move(from, to, promote = true)
            if (!mustPromote(piece.type, piece.owner, to)) moves += Move(from, to)
        }
        return moves.filter { !apply(it).isInCheck(turn) }
    }

    /** 持ち駒 type を打つ合法手。 */
    fun legalDrops(type: PieceType): List<Move> {
        if (handCount(turn, type) == 0) return emptyList()
        return Square.ALL
            .filter { board[it.index] == null }
            .filter { !mustPromote(type, turn, it) }
            .filter { type != PieceType.FU || !hasPawnOnFile(turn, it.col) }
            .map { Move.drop(type, it) }
            .filter { move ->
                val next = apply(move)
                !next.isInCheck(turn) && !(type == PieceType.FU && next.isCheckmate())
            }
    }

    fun isLegal(move: Move): Boolean =
        if (move.from == null) move in legalDrops(requireNotNull(move.dropType))
        else move in legalMovesFrom(move.from)

    /** 手番側が詰んでいるか。 */
    fun isCheckmate(): Boolean = isInCheck(turn) && !hasAnyLegalMove()

    private fun hasAnyLegalMove(): Boolean =
        Square.ALL.any { legalMovesFrom(it).isNotEmpty() } ||
            handOf(turn).keys.any { type ->
                // 打ち歩詰めの判定で再帰しないよう、打ち手は自玉の安全のみ確認する
                Square.ALL.any { sq ->
                    board[sq.index] == null &&
                        !mustPromote(type, turn, sq) &&
                        (type != PieceType.FU || !hasPawnOnFile(turn, sq.col)) &&
                        !apply(Move.drop(type, sq)).isInCheck(turn)
                }
            }

    private fun hasPawnOnFile(player: Player, col: Int): Boolean =
        (0..8).any { row -> board[row * 9 + col] == Piece(PieceType.FU, player) }

    /** SFEN 形式の局面文字列(盤面・手番・持ち駒)。千日手判定などに使う。 */
    fun toSfen(): String {
        val sb = StringBuilder()
        for (row in 0..8) {
            var empty = 0
            for (col in 0..8) {
                val p = board[row * 9 + col]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    if (p.type.isPromoted) sb.append('+')
                    val c = SFEN_CHARS.getValue(p.type.unpromoted)
                    sb.append(if (p.owner == Player.SENTE) c.uppercaseChar() else c)
                }
            }
            if (empty > 0) sb.append(empty)
            if (row < 8) sb.append('/')
        }
        sb.append(if (turn == Player.SENTE) " b " else " w ")
        val handPart = StringBuilder()
        for (player in Player.entries) {
            for (type in PieceType.HAND_ORDER) {
                val n = handCount(player, type)
                if (n == 0) continue
                if (n > 1) handPart.append(n)
                val c = SFEN_CHARS.getValue(type)
                handPart.append(if (player == Player.SENTE) c.uppercaseChar() else c)
            }
        }
        sb.append(if (handPart.isEmpty()) "-" else handPart)
        return sb.toString()
    }

    companion object {
        private val ORTHOGONALS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        private val DIAGONALS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        private val KING_DIRS = ORTHOGONALS + DIAGONALS

        private val SFEN_CHARS = mapOf(
            PieceType.FU to 'p', PieceType.KYO to 'l', PieceType.KEI to 'n', PieceType.GIN to 's',
            PieceType.KIN to 'g', PieceType.KAKU to 'b', PieceType.HI to 'r', PieceType.OU to 'k',
        )

        /** 敵陣(先手から見て一〜三段目)にいるか。 */
        fun inPromotionZone(player: Player, sq: Square): Boolean =
            if (player == Player.SENTE) sq.row <= 2 else sq.row >= 6

        fun canPromote(piece: Piece, from: Square, to: Square): Boolean =
            piece.type.canPromote &&
                (inPromotionZone(piece.owner, from) || inPromotionZone(piece.owner, to))

        /** 行き所のない駒になる場合は成りが必須(打つことも不可)。 */
        fun mustPromote(type: PieceType, owner: Player, to: Square): Boolean {
            val distanceToEnd = if (owner == Player.SENTE) to.row else 8 - to.row
            return when (type) {
                PieceType.FU, PieceType.KYO -> distanceToEnd == 0
                PieceType.KEI -> distanceToEnd <= 1
                else -> false
            }
        }

        /** 平手の初期局面。 */
        fun initial(): Position = fromSfen(
            "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b -"
        )

        /** SFEN 文字列(手数部分は省略可)から局面を作る。 */
        fun fromSfen(sfen: String): Position {
            val parts = sfen.trim().split(" ")
            require(parts.size >= 2) { "不正なSFEN: $sfen" }
            val board = arrayOfNulls<Piece>(81)
            val charToType = SFEN_CHARS.entries.associate { (k, v) -> v to k }
            parts[0].split("/").forEachIndexed { row, line ->
                var col = 0
                var promoted = false
                for (ch in line) {
                    when {
                        ch.isDigit() -> col += ch.digitToInt()
                        ch == '+' -> promoted = true
                        else -> {
                            val base = requireNotNull(charToType[ch.lowercaseChar()]) { "不正な駒: $ch" }
                            val type = if (promoted) requireNotNull(base.promoted) else base
                            val owner = if (ch.isUpperCase()) Player.SENTE else Player.GOTE
                            board[row * 9 + col] = Piece(type, owner)
                            col++
                            promoted = false
                        }
                    }
                }
            }
            val turn = if (parts[1] == "b") Player.SENTE else Player.GOTE
            val hands = Player.entries.associateWith { mutableMapOf<PieceType, Int>() }
            val handStr = parts.getOrElse(2) { "-" }
            if (handStr != "-") {
                var count = 0
                for (ch in handStr) {
                    if (ch.isDigit()) {
                        count = count * 10 + ch.digitToInt()
                    } else {
                        val type = requireNotNull(charToType[ch.lowercaseChar()]) { "不正な持ち駒: $ch" }
                        val owner = if (ch.isUpperCase()) Player.SENTE else Player.GOTE
                        hands.getValue(owner)[type] = if (count == 0) 1 else count
                        count = 0
                    }
                }
            }
            return Position(board, hands, turn)
        }
    }
}

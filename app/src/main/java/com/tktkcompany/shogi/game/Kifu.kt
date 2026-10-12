package com.tktkcompany.shogi.game

/**
 * 日本式の棋譜表記(例: "▲７六歩", "△同　銀", "▲２二角成", "△５五桂打")。
 * 同じ種類の駒が複数動ける場合の「右・左・上・引」などの区別は省略している。
 */
object Kifu {
    private const val FILES = "０１２３４５６７８９"
    private const val RANKS = "〇一二三四五六七八九"

    fun format(before: Position, move: Move, previous: Move?): String {
        val mark = if (before.turn == Player.SENTE) "▲" else "△"
        val dest = if (previous?.to == move.to) "同　" else "${FILES[move.to.file]}${RANKS[move.to.rank]}"
        if (move.from == null) {
            val type = requireNotNull(move.dropType)
            // 打たなくても同じマスへ動ける駒があれば「打」を付けるのが正式だが、分かりやすさのため常に付ける
            return "$mark$dest${name(type)}打"
        }
        val piece = requireNotNull(before[move.from])
        val suffix = when {
            move.promote -> "成"
            Position.canPromote(piece, move.from, move.to) -> "不成"
            else -> ""
        }
        return "$mark$dest${name(piece.type)}$suffix"
    }

    fun name(type: PieceType): String = when (type) {
        PieceType.NARIKYO -> "成香"
        PieceType.NARIKEI -> "成桂"
        PieceType.NARIGIN -> "成銀"
        else -> type.kanji
    }
}

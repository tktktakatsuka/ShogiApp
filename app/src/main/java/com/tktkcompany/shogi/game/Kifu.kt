package com.tktkcompany.shogi.game

/** 棋譜の表記のしかた。 */
enum class KifuStyle {
    /** 日本式(例: "▲７六歩", "△同　銀", "▲２二角成", "△５五桂打")。 */
    JAPANESE,

    /** 英語圏で使われる表記(例: "▲P-7f", "△Sx2b", "▲Bx2b+", "△N*5e")。 */
    WESTERN,
}

/**
 * 棋譜表記。
 * 同じ種類の駒が複数動ける場合の「右・左・上・引」などの区別(英語式では移動元のマス)は省略している。
 */
object Kifu {
    private const val FILES = "０１２３４５６７８９"
    private const val RANKS = "〇一二三四五六七八九"

    fun format(before: Position, move: Move, previous: Move?, style: KifuStyle = KifuStyle.JAPANESE): String {
        val mark = if (before.turn == Player.SENTE) "▲" else "△"
        return mark + when (style) {
            KifuStyle.JAPANESE -> japanese(before, move, previous)
            KifuStyle.WESTERN -> western(before, move)
        }
    }

    private fun japanese(before: Position, move: Move, previous: Move?): String {
        val dest = if (previous?.to == move.to) "同　" else "${FILES[move.to.file]}${RANKS[move.to.rank]}"
        if (move.from == null) {
            val type = requireNotNull(move.dropType)
            // 打たなくても同じマスへ動ける駒があれば「打」を付けるのが正式だが、分かりやすさのため常に付ける
            return "$dest${name(type)}打"
        }
        val piece = requireNotNull(before[move.from])
        val suffix = when {
            move.promote -> "成"
            Position.canPromote(piece, move.from, move.to) -> "不成"
            else -> ""
        }
        return "$dest${name(piece.type)}$suffix"
    }

    private fun western(before: Position, move: Move): String {
        val dest = "${move.to.file}${'a' + move.to.row}"
        if (move.from == null) return "${letter(requireNotNull(move.dropType))}*$dest"
        val piece = requireNotNull(before[move.from])
        val action = if (before[move.to] != null) "x" else "-"
        val suffix = when {
            move.promote -> "+"
            Position.canPromote(piece, move.from, move.to) -> "="
            else -> ""
        }
        return "${letter(piece.type)}$action$dest$suffix"
    }

    fun name(type: PieceType): String = when (type) {
        PieceType.NARIKYO -> "成香"
        PieceType.NARIKEI -> "成桂"
        PieceType.NARIGIN -> "成銀"
        else -> type.kanji
    }

    /** 英語式の駒の記号。成駒は元の駒の記号に "+" を付ける。 */
    private fun letter(type: PieceType): String {
        val base = when (type.unpromoted) {
            PieceType.FU -> "P"
            PieceType.KYO -> "L"
            PieceType.KEI -> "N"
            PieceType.GIN -> "S"
            PieceType.KIN -> "G"
            PieceType.KAKU -> "B"
            PieceType.HI -> "R"
            else -> "K"
        }
        return if (type.isPromoted) "+$base" else base
    }
}

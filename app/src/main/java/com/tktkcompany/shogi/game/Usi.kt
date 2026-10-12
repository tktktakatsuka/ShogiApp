package com.tktkcompany.shogi.game

/**
 * 指し手の USI 形式表記。オンライン対戦で指し手を送受信するのに使う。
 * 例: "7g7f"(７六へ移動)、"8h2b+"(成り)、"P*5e"(歩を打つ)
 */
object Usi {
    private val DROP_CHARS = mapOf(
        PieceType.FU to 'P', PieceType.KYO to 'L', PieceType.KEI to 'N', PieceType.GIN to 'S',
        PieceType.KIN to 'G', PieceType.KAKU to 'B', PieceType.HI to 'R',
    )
    private val CHAR_TO_DROP = DROP_CHARS.entries.associate { (k, v) -> v to k }

    fun format(move: Move): String {
        val to = square(move.to)
        return if (move.from == null) {
            "${DROP_CHARS.getValue(requireNotNull(move.dropType))}*$to"
        } else {
            square(move.from) + to + if (move.promote) "+" else ""
        }
    }

    /** 文字列を指し手に変換する。形式が不正なら null。 */
    fun parse(text: String): Move? {
        if (text.length == 4 && text[1] == '*') {
            val type = CHAR_TO_DROP[text[0]] ?: return null
            val to = parseSquare(text.substring(2, 4)) ?: return null
            return Move.drop(type, to)
        }
        if (text.length != 4 && !(text.length == 5 && text[4] == '+')) return null
        val from = parseSquare(text.substring(0, 2)) ?: return null
        val to = parseSquare(text.substring(2, 4)) ?: return null
        return Move(from, to, promote = text.length == 5)
    }

    private fun square(sq: Square): String = "${sq.file}${'a' + sq.row}"

    private fun parseSquare(s: String): Square? {
        val file = s[0].digitToIntOrNull() ?: return null
        val row = s[1] - 'a'
        return Square(row, 9 - file).takeIf { file in 1..9 && it.isOnBoard }
    }
}

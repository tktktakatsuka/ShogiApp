package com.tktkcompany.shogi.game

import kotlin.math.abs

/**
 * 検討エンジンが読んだ結果。評価値と詰みは「その局面の手番側」から見た値。
 * [pv] は読み筋(先頭が最善手)。
 */
data class Analysis(
    val depth: Int,
    /** 評価値(歩1枚がおよそ100)。詰みを読み切ったときは null。 */
    val scoreCp: Int?,
    val mate: Mate?,
    val pv: List<Move>,
) {
    /** 詰みの読み切り。[win] は手番側が勝つかどうか、[moves] は詰みまでの手数(不明なら null)。 */
    data class Mate(val win: Boolean, val moves: Int?)

    val bestMove: Move? get() = pv.firstOrNull()

    /** 先手から見た形勢の説明(例: "+350 先手有利"、"後手の勝ち(7手詰)")。 */
    fun describe(turn: Player): String {
        if (mate != null) {
            val winner = if (mate.win) turn else turn.opponent
            return "${winner.label}の勝ち" + (mate.moves?.let { "(${it}手詰)" } ?: "(詰み)")
        }
        val cp = scoreCp ?: return ""
        val forSente = if (turn == Player.SENTE) cp else -cp
        val side = if (forSente >= 0) Player.SENTE else Player.GOTE
        val label = when (abs(forSente)) {
            in 0 until 200 -> "互角"
            in 200 until 800 -> "${side.label}有利"
            in 800 until 2000 -> "${side.label}優勢"
            else -> "${side.label}勝勢"
        }
        return "${if (forSente > 0) "+" else ""}$forSente $label"
    }

    companion object {
        /**
         * USI の "info ..." の行を読み取る。読み筋のない行や、探索途中の仮の値
         * (lowerbound / upperbound)は null。
         * 例: "info depth 18 seldepth 33 score cp 140 nodes 514068 pv 2g2f 3c3d"
         */
        fun parseInfo(line: String): Analysis? {
            val tokens = line.trim().split(Regex("\\s+"))
            if (tokens.firstOrNull() != "info") return null
            var depth = 0
            var cp: Int? = null
            var mate: Mate? = null
            var pv = emptyList<Move>()
            var i = 1
            while (i < tokens.size) {
                when (tokens[i]) {
                    "string" -> return null
                    "lowerbound", "upperbound" -> return null
                    "depth" -> depth = tokens.getOrNull(++i)?.toIntOrNull() ?: 0
                    "score" -> {
                        val kind = tokens.getOrNull(++i)
                        val value = tokens.getOrNull(++i) ?: return null
                        when (kind) {
                            "cp" -> cp = value.toIntOrNull() ?: return null
                            "mate" -> mate = Mate(
                                win = !value.startsWith("-"),
                                moves = value.toIntOrNull()?.let(::abs)?.takeIf { it > 0 },
                            )
                            else -> return null
                        }
                    }
                    "pv" -> {
                        pv = tokens.drop(i + 1).map { Usi.parse(it) }.takeWhile { it != null }.filterNotNull()
                        i = tokens.size
                    }
                }
                i++
            }
            if (pv.isEmpty() || (cp == null && mate == null)) return null
            return Analysis(depth, cp, mate, pv)
        }
    }
}

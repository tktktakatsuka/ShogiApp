package com.tktkcompany.shogi.game

import kotlin.random.Random

/**
 * CPU の強さ。[INTRO] はアプリ内蔵のかんたんな思考、それ以外は将棋エンジン(やねうら王)を使う。
 *
 * エンジンは本来とても強いので、読む量([nodes] / [byoyomiMillis])を減らし、
 * 最善手から [marginCp] 以内の候補手([multiPv] 手まで)からランダムに選ばせて弱くする。
 */
enum class CpuLevel(
    val usesEngine: Boolean,
    val nodes: Long? = null,
    val byoyomiMillis: Long? = null,
    val multiPv: Int = 1,
    val marginCp: Int = 0,
) {
    INTRO(usesEngine = false),
    NOVICE(usesEngine = true, nodes = 2_000, multiPv = 6, marginCp = 600),
    INTERMEDIATE(usesEngine = true, nodes = 15_000, multiPv = 4, marginCp = 250),
    ADVANCED(usesEngine = true, nodes = 120_000, multiPv = 3, marginCp = 80),
    EXPERT(usesEngine = true, byoyomiMillis = 2_000);

    /** USI の go コマンド。 */
    val goCommand: String
        get() = when {
            nodes != null -> "go nodes $nodes"
            else -> "go btime 0 wtime 0 byoyomi ${byoyomiMillis ?: 1_000}"
        }

    companion object {
        /**
         * エンジンが返した候補手([candidates]、MultiPV の順)から指す手を選ぶ。
         * 詰みを読み切った手は必ず指す。それ以外は最善の評価値から [CpuLevel.marginCp] 以内の手からランダム。
         */
        fun pick(level: CpuLevel, candidates: List<Analysis>, random: Random = Random.Default): Move? {
            val scored = candidates.mapNotNull { a -> a.bestMove?.let { it to score(a) } }
            if (scored.isEmpty()) return null
            val best = scored.maxOf { it.second }
            if (best >= MATE_SCORE / 2) return scored.first { it.second == best }.first
            val eligible = scored.filter { it.second >= best - level.marginCp }
            return eligible[random.nextInt(eligible.size)].first
        }

        private const val MATE_SCORE = 100_000

        /** 手番側から見た点数。勝ちの詰みは大きく、負けの詰みは小さく。 */
        private fun score(a: Analysis): Int {
            val mate = a.mate
            return when {
                mate == null -> a.scoreCp ?: 0
                mate.win -> MATE_SCORE - (mate.moves ?: 0)
                else -> -MATE_SCORE + (mate.moves ?: 0)
            }
        }
    }
}

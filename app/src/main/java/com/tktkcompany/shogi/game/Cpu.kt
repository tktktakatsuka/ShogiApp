package com.tktkcompany.shogi.game

import kotlin.random.Random

/**
 * かんたんなCPU。駒得を基準に2手先まで読む。
 */
class Cpu(private val random: Random = Random.Default) {

    fun chooseMove(position: Position): Move? {
        val moves = position.legalMoves()
        if (moves.isEmpty()) return null
        val me = position.turn
        var best = Int.MIN_VALUE
        val bestMoves = mutableListOf<Move>()
        for (move in moves) {
            val next = position.apply(move)
            val score = if (next.isCheckmate()) {
                MATE
            } else {
                // 相手の最善応手(駒得基準)を引いた評価
                val replies = next.legalMoves()
                if (replies.isEmpty()) evaluate(next, me)
                else replies.minOf { evaluate(next.apply(it), me) }
            }
            if (score > best) {
                best = score
                bestMoves.clear()
                bestMoves += move
            } else if (score == best) {
                bestMoves += move
            }
        }
        return bestMoves[random.nextInt(bestMoves.size)]
    }

    private fun evaluate(position: Position, me: Player): Int {
        var score = 0
        for (sq in Square.ALL) {
            val p = position[sq] ?: continue
            if (p.type == PieceType.OU) continue
            score += if (p.owner == me) p.type.value else -p.type.value
        }
        for (player in Player.entries) {
            for ((type, n) in position.handOf(player)) {
                val v = type.value * n
                score += if (player == me) v else -v
            }
        }
        return score
    }

    private companion object {
        const val MATE = 100_000
    }
}

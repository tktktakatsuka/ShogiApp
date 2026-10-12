package com.tktkcompany.shogi.online

import com.tktkcompany.shogi.game.Game
import com.tktkcompany.shogi.game.Player
import com.tktkcompany.shogi.game.Usi

enum class RoomStatus { WAITING, PLAYING, FINISHED }

/** サーバー上に保存された1手。by は指したプレイヤーのユーザーID。 */
data class RemoteMove(val usi: String, val by: String)

/**
 * 対局部屋の状態。部屋を作った人(host)が先手、参加した人(guest)が後手。
 */
data class Room(
    val code: String,
    val hostUid: String,
    val guestUid: String?,
    val status: RoomStatus,
    val moves: List<RemoteMove>,
    val resignedBy: String?,
) {
    fun colorOf(uid: String): Player? = when (uid) {
        hostUid -> Player.SENTE
        guestUid -> Player.GOTE
        else -> null
    }

    fun uidOf(player: Player): String? = if (player == Player.SENTE) hostUid else guestUid

    /**
     * サーバーの指し手を再生して対局を復元する。
     * [current] の指し手がサーバーの指し手の先頭部分と一致していれば、その続きだけを再生する。
     * 手番違い・反則手が含まれていれば、その手の直前で止めて [ReplayResult.invalidAt] に記録する。
     */
    fun replay(current: Game? = null): ReplayResult {
        val game = current?.takeIf { isPrefixOfMoves(it) } ?: Game()
        for (index in game.moveCount until moves.size) {
            val remote = moves[index]
            val expected = uidOf(game.position.turn)
            val move = Usi.parse(remote.usi)
            if (remote.by != expected || move == null || !game.play(move)) {
                return ReplayResult(game, invalidAt = index)
            }
        }
        resignedBy?.let(::colorOf)?.let(game::resign)
        return ReplayResult(game, invalidAt = null)
    }

    private fun isPrefixOfMoves(game: Game): Boolean =
        game.moveCount <= moves.size &&
            game.moves.indices.all { Usi.format(game.moves[it]) == moves[it].usi }
}

data class ReplayResult(val game: Game, val invalidAt: Int?)

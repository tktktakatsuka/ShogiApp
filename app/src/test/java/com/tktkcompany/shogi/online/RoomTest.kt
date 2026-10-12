package com.tktkcompany.shogi.online

import com.tktkcompany.shogi.game.EndReason
import com.tktkcompany.shogi.game.Game
import com.tktkcompany.shogi.game.GameResult
import com.tktkcompany.shogi.game.Player
import com.tktkcompany.shogi.game.Usi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RoomTest {
    private fun room(vararg moves: Pair<String, String>, resignedBy: String? = null) = Room(
        code = "123456",
        hostUid = "host",
        guestUid = "guest",
        status = RoomStatus.PLAYING,
        moves = moves.map { (usi, by) -> RemoteMove(usi, by) },
        resignedBy = resignedBy,
    )

    @Test
    fun colorsAreAssignedByRole() {
        val r = room()
        assertEquals(Player.SENTE, r.colorOf("host"))
        assertEquals(Player.GOTE, r.colorOf("guest"))
        assertNull(r.colorOf("someone"))
    }

    @Test
    fun replaysMoves() {
        val result = room("7g7f" to "host", "3c3d" to "guest", "8h2b+" to "host").replay()
        assertNull(result.invalidAt)
        assertEquals(3, result.game.moveCount)
        assertEquals("8h2b+", Usi.format(result.game.lastMove!!))
    }

    @Test
    fun stopsAtMoveByWrongPlayer() {
        val result = room("7g7f" to "host", "3c3d" to "host").replay()
        assertEquals(1, result.invalidAt)
        assertEquals(1, result.game.moveCount)
    }

    @Test
    fun stopsAtIllegalMove() {
        val result = room("7g7f" to "host", "5a5c" to "guest").replay()
        assertEquals(1, result.invalidAt)
    }

    @Test
    fun appliesResignation() {
        val result = room("7g7f" to "host", resignedBy = "guest").replay()
        assertEquals(GameResult(Player.SENTE, EndReason.RESIGN), result.game.result)
    }

    @Test
    fun continuesFromMatchingGame() {
        val current = Game()
        current.play(Usi.parse("7g7f")!!)
        val result = room("7g7f" to "host", "3c3d" to "guest").replay(current)
        assertSame(current, result.game)
        assertEquals(2, current.moveCount)
    }

    @Test
    fun rebuildsWhenLocalGameDiverges() {
        // 送信に失敗して取り消された手がローカルにだけ残っている場合など
        val current = Game()
        current.play(Usi.parse("2g2f")!!)
        val result = room("7g7f" to "host").replay(current)
        assertEquals(1, result.game.moveCount)
        assertEquals("7g7f", Usi.format(result.game.lastMove!!))
    }
}

package com.tktkcompany.shogi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsiTest {
    @Test
    fun formatsAndParsesMoves() {
        val cases = mapOf(
            "7g7f" to Move(Square(6, 2), Square(5, 2)),
            "8h2b+" to Move(Square(7, 1), Square(1, 7), promote = true),
            "P*5e" to Move.drop(PieceType.FU, Square(4, 4)),
            "R*1a" to Move.drop(PieceType.HI, Square(0, 8)),
        )
        for ((text, move) in cases) {
            assertEquals(text, Usi.format(move))
            assertEquals(move, Usi.parse(text))
        }
    }

    @Test
    fun rejectsMalformedText() {
        listOf("", "7g7", "0a1a", "7j7f", "K*5e", "7g7f=", "7g7f++").forEach {
            assertNull(it, Usi.parse(it))
        }
    }

    @Test
    fun everyLegalMoveRoundTrips() {
        val game = Game()
        repeat(30) {
            val moves = game.position.legalMoves()
            moves.forEach { assertEquals(it, Usi.parse(Usi.format(it))) }
            game.play(moves[(it * 7) % moves.size])
        }
    }
}

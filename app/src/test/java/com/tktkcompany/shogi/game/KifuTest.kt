package com.tktkcompany.shogi.game

import org.junit.Assert.assertEquals
import org.junit.Test

class KifuTest {
    private fun playAll(vararg usi: String): Game {
        val game = Game()
        usi.forEach { check(game.play(Usi.parse(it)!!)) { "illegal: $it" } }
        return game
    }

    @Test
    fun formatsOrdinaryMoves() {
        assertEquals("▲７六歩", playAll("7g7f").lastMoveNotation)
        assertEquals("△３四歩", playAll("7g7f", "3c3d").lastMoveNotation)
    }

    @Test
    fun formatsPromotionAndSameSquare() {
        assertEquals("▲２二角成", playAll("7g7f", "3c3d", "8h2b+").lastMoveNotation)
        assertEquals("△同　銀", playAll("7g7f", "3c3d", "8h2b+", "3a2b").lastMoveNotation)
    }

    @Test
    fun formatsNonPromotionAndDrop() {
        assertEquals("▲２二角不成", playAll("7g7f", "3c3d", "8h2b").lastMoveNotation)
        assertEquals(
            "▲５五角打",
            playAll("7g7f", "3c3d", "8h2b+", "3a2b", "B*5e").lastMoveNotation,
        )
    }

    @Test
    fun noNotationBeforeFirstMove() {
        assertEquals(null, Game().lastMoveNotation)
    }
}

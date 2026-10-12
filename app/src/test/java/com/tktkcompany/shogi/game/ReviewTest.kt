package com.tktkcompany.shogi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTest {
    private fun usi(vararg moves: String) = moves.map { Usi.parse(it)!! }
    private val main = usi("7g7f", "3c3d", "8h2b+", "3a2b")
    private val review = Review(main)

    @Test
    fun navigatesMainLine() {
        var s = review.end()
        assertEquals(4, s.index)
        assertEquals("△同　銀", review.view(s).lastMoveNotation)
        s = review.back(s)
        assertEquals("▲２二角成", review.view(s).lastMoveNotation)
        s = review.jumpTo(0)
        assertNull(review.view(s).lastMoveNotation)
        assertEquals(Position.initial().toSfen(), review.view(s).position.toSfen())
        assertEquals(1, review.forward(s).index)
        assertEquals(4, review.forward(review.end()).index)
    }

    @Test
    fun playingMainMoveFollowsMainLine() {
        val s = review.play(review.jumpTo(1), usi("3c3d").single())!!
        assertEquals(ReviewState(2), s)
    }

    @Test
    fun differentMoveStartsVariation() {
        var s = review.play(review.jumpTo(1), usi("8c8d").single())!!
        assertTrue(s.isVariation)
        s = review.play(s, usi("2g2f").single())!!
        val view = review.view(s)
        assertEquals(listOf("△８四歩", "▲２六歩"), view.variationNotations)
        assertEquals(3, view.ply)
        // 変化中の「戻る」は変化の手を取り消す
        assertEquals(1, review.back(s).variation.size)
        assertEquals(ReviewState(1), review.backToMainLine(s))
        // 変化中は本譜を進めない
        assertEquals(s, review.forward(s))
    }

    @Test
    fun rejectsIllegalMoves() {
        assertNull(review.play(review.jumpTo(0), usi("5i5g").single()))
    }

    @Test
    fun normalizeDropsInvalidRemoteState() {
        val bad = ReviewState(index = 99, variation = usi("7g7f"))
        // 99 → 4(最終局面)。後手番なので ▲7六歩 は反則 → 取り除かれる
        assertEquals(ReviewState(4), review.normalize(bad))
    }
}

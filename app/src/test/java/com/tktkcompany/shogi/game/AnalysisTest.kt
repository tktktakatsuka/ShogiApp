package com.tktkcompany.shogi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisTest {
    @Test
    fun `評価値と読み筋を読み取る`() {
        val a = Analysis.parseInfo(
            "info depth 18 seldepth 33 multipv 1 score cp 140 nodes 514068 nps 1557781 hashfull 20 time 330 pv 2g2f 3c3d P*5e 8h2b+"
        )!!
        assertEquals(18, a.depth)
        assertEquals(140, a.scoreCp)
        assertNull(a.mate)
        assertEquals(listOf("2g2f", "3c3d", "P*5e", "8h2b+"), a.pv.map(Usi::format))
        assertEquals("2g2f", Usi.format(a.bestMove!!))
    }

    @Test
    fun `詰みを読み取る`() {
        assertEquals(Analysis.Mate(win = true, moves = 5), Analysis.parseInfo("info depth 9 score mate 5 pv 2g2f")!!.mate)
        assertEquals(Analysis.Mate(win = false, moves = 4), Analysis.parseInfo("info depth 9 score mate -4 pv 2g2f")!!.mate)
        assertEquals(Analysis.Mate(win = true, moves = null), Analysis.parseInfo("info score mate + pv 2g2f")!!.mate)
        assertEquals(Analysis.Mate(win = false, moves = null), Analysis.parseInfo("info score mate - pv 2g2f")!!.mate)
    }

    @Test
    fun `読み筋のない行や仮の評価値は無視する`() {
        listOf(
            "info string loading eval file : <internal>",
            "info depth 18 score cp 143 lowerbound nodes 589768 pv 2g2f 3c3d",
            "info depth 18 score cp 143 upperbound nodes 589768 pv 2g2f 3c3d",
            "info depth 3 nodes 100",
            "info depth 3 score cp 10",
            "bestmove 2g2f ponder 3c3d",
            "readyok",
            "",
        ).forEach { assertNull(it, Analysis.parseInfo(it)) }
    }

    @Test
    fun `読み筋は不正な手の手前で打ち切る`() {
        val a = Analysis.parseInfo("info depth 2 score cp 0 pv 2g2f resign 3c3d")!!
        assertEquals(listOf("2g2f"), a.pv.map(Usi::format))
    }

    @Test
    fun `形勢は先手から見た値で説明する`() {
        fun cp(value: Int) = Analysis(depth = 1, scoreCp = value, mate = null, pv = emptyList())
        assertEquals("+120 互角", cp(120).describe(Player.SENTE))
        assertEquals("-120 互角", cp(120).describe(Player.GOTE))
        assertEquals("0 互角", cp(0).describe(Player.GOTE))
        assertEquals("+350 先手有利", cp(350).describe(Player.SENTE))
        assertEquals("+350 先手有利", cp(-350).describe(Player.GOTE))
        assertEquals("-1200 後手優勢", cp(-1200).describe(Player.SENTE))
        assertEquals("-3000 後手勝勢", cp(3000).describe(Player.GOTE))
    }

    @Test
    fun `詰みは勝つ側と手数で説明する`() {
        fun mate(win: Boolean, moves: Int?) = Analysis(1, null, Analysis.Mate(win, moves), emptyList())
        assertEquals("先手の勝ち(5手詰)", mate(true, 5).describe(Player.SENTE))
        assertEquals("後手の勝ち(4手詰)", mate(false, 4).describe(Player.SENTE))
        assertEquals("後手の勝ち(詰み)", mate(true, null).describe(Player.GOTE))
    }
}

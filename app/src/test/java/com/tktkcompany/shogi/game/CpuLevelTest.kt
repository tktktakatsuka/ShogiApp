package com.tktkcompany.shogi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CpuLevelTest {
    private fun cand(usi: String, cp: Int? = null, mate: Analysis.Mate? = null) =
        Analysis(depth = 10, scoreCp = cp, mate = mate, pv = listOf(Usi.parse(usi)!!))

    private val candidates = listOf(
        cand("7g7f", cp = 100),
        cand("2g2f", cp = 60),
        cand("5g5f", cp = -300),
        cand("1g1f", cp = -900),
    )

    @Test
    fun strongestLevelAlwaysPlaysBestMove() {
        repeat(20) {
            assertEquals(Usi.parse("7g7f"), CpuLevel.pick(CpuLevel.EXPERT, candidates, Random(it)))
        }
    }

    @Test
    fun weakerLevelsChooseOnlyWithinMargin() {
        val advanced = (0 until 50).map { CpuLevel.pick(CpuLevel.ADVANCED, candidates, Random(it)) }.toSet()
        assertEquals(setOf(Usi.parse("7g7f"), Usi.parse("2g2f")), advanced)
        val novice = (0 until 200).map { CpuLevel.pick(CpuLevel.NOVICE, candidates, Random(it)) }.toSet()
        assertTrue(Usi.parse("5g5f") in novice)
        // 600 を超えて悪い手は初級でも選ばない
        assertTrue(Usi.parse("1g1f") !in novice)
    }

    @Test
    fun alwaysTakesWinningMate() {
        val withMate = candidates + cand("8h2b+", mate = Analysis.Mate(win = true, moves = 3))
        repeat(20) {
            assertEquals(Usi.parse("8h2b+"), CpuLevel.pick(CpuLevel.NOVICE, withMate, Random(it)))
        }
    }

    @Test
    fun avoidsLosingMateWhenPossible() {
        val list = listOf(cand("7g7f", mate = Analysis.Mate(win = false, moves = 5)), cand("2g2f", cp = -1500))
        repeat(20) { assertEquals(Usi.parse("2g2f"), CpuLevel.pick(CpuLevel.NOVICE, list, Random(it))) }
    }

    @Test
    fun noCandidates() {
        assertNull(CpuLevel.pick(CpuLevel.NOVICE, emptyList()))
    }

    @Test
    fun goCommands() {
        assertEquals("go nodes 15000", CpuLevel.INTERMEDIATE.goCommand)
        assertEquals("go btime 0 wtime 0 byoyomi 2000", CpuLevel.EXPERT.goCommand)
    }
}

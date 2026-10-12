package com.tktkcompany.shogi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionTest {

    /** 筋・段(例: 7, 6 → ７六)からマスを作る。 */
    private fun sq(file: Int, rank: Int) = Square(rank - 1, 9 - file)

    @Test
    fun initialPositionHas30LegalMoves() {
        assertEquals(30, Position.initial().legalMoves().size)
    }

    @Test
    fun sfenRoundTrip() {
        val sfen = "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b -"
        assertEquals(sfen, Position.fromSfen(sfen).toSfen())
        val withHands = "4k4/9/9/9/9/9/9/9/4K4 w 2Pb"
        assertEquals(withHands, Position.fromSfen(withHands).toSfen())
    }

    @Test
    fun captureGoesToHandUnpromoted() {
        // 先手の飛車が後手の と金 を取る
        val pos = Position.fromSfen("4k4/9/9/9/4+p4/9/9/4R4/K8 b -")
        val next = pos.apply(Move(sq(5, 8), sq(5, 5)))
        assertEquals(1, next.handCount(Player.SENTE, PieceType.FU))
        assertEquals(Piece(PieceType.HI, Player.SENTE), next[sq(5, 5)])
        assertEquals(Player.GOTE, next.turn)
    }

    @Test
    fun pawnMustPromoteOnLastRank() {
        val pos = Position.fromSfen("k8/9/4P4/9/9/9/9/9/8K b -")
        val moves = pos.legalMovesFrom(sq(5, 3))
        // 二段目へは成・不成の両方を選べる
        assertEquals(setOf(true, false), moves.map { it.promote }.toSet())
        val pos2 = Position.fromSfen("k8/4P4/9/9/9/9/9/9/8K b -")
        val moves2 = pos2.legalMovesFrom(sq(5, 2))
        assertEquals(listOf(Move(sq(5, 2), sq(5, 1), promote = true)), moves2)
    }

    @Test
    fun knightCannotBeDroppedOnLastTwoRanks() {
        val pos = Position.fromSfen("k8/9/9/9/9/9/9/9/8K b N")
        val drops = pos.legalDrops(PieceType.KEI)
        assertTrue(drops.none { it.to.rank <= 2 })
        assertTrue(drops.any { it.to.rank == 3 })
    }

    @Test
    fun nifuIsIllegal() {
        val pos = Position.fromSfen("k8/9/9/9/9/9/4P4/9/8K b P")
        val drops = pos.legalDrops(PieceType.FU)
        assertTrue(drops.none { it.to.file == 5 })
        assertTrue(drops.any { it.to.file == 4 })
    }

    @Test
    fun cannotLeaveKingInCheck() {
        // 先手玉が後手飛車に王手されている。金はピンされていて動けない。
        val pos = Position.fromSfen("4r3k/9/9/9/9/9/9/4G4/4K4 b -")
        assertTrue(pos.isInCheck(Player.SENTE).not())
        val pinned = pos.legalMovesFrom(sq(5, 8))
        assertTrue(pinned.all { it.to.file == 5 })
    }

    @Test
    fun detectsCheckmate() {
        // 頭金の詰み
        val pos = Position.fromSfen("4k4/9/4P4/9/9/9/9/9/8K b G")
        val mate = Move.drop(PieceType.KIN, sq(5, 2))
        assertTrue(pos.isLegal(mate))
        val next = pos.apply(mate)
        assertTrue(next.isInCheck(Player.GOTE))
        assertTrue(next.isCheckmate())
    }

    @Test
    fun pawnDropMateIsIllegal() {
        // 打ち歩詰め: 1二歩打は詰みになるので打てない(2三銀が歩と2二を守る)
        val pos = Position.fromSfen("7lk/9/7S1/9/9/9/9/9/K8 b P")
        assertFalse(pos.isLegal(Move.drop(PieceType.FU, sq(1, 2))))
        // 突き歩詰めは反則ではない
        val pushed = Position.fromSfen("7lk/9/7SP/9/9/9/9/9/K8 b -")
        val push = Move(sq(1, 3), sq(1, 2))
        assertTrue(pushed.isLegal(push))
        assertTrue(pushed.apply(push).isCheckmate())
        // 歩で王手するだけなら打てる(玉が逃げられる)
        val pos2 = Position.fromSfen("4k4/9/9/9/9/9/9/9/8K b P")
        assertTrue(pos2.isLegal(Move.drop(PieceType.FU, sq(5, 2))))
    }

    @Test
    fun gameDetectsWinner() {
        val game = Game(Position.fromSfen("4k4/9/4P4/9/9/9/9/9/8K b G"))
        assertTrue(game.play(Move.drop(PieceType.KIN, sq(5, 2))))
        assertEquals(GameResult(Player.SENTE, EndReason.CHECKMATE), game.result)
        assertTrue(game.undo())
        assertEquals(null, game.result)
    }

    @Test
    fun cpuFindsMateInOne() {
        val pos = Position.fromSfen("4k4/9/4P4/9/9/9/9/9/8K b G")
        val move = Cpu().chooseMove(pos)
        assertNotNull(move)
        assertTrue(pos.apply(move!!).isCheckmate())
    }

    @Test
    fun cpuMoveFromInitialPositionIsLegal() {
        val pos = Position.initial()
        val move = Cpu().chooseMove(pos)
        assertTrue(pos.isLegal(move!!))
    }
}

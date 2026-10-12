package com.tktkcompany.shogi.game

/** 手番。先手(SENTE)は盤の下側、後手(GOTE)は上側。 */
enum class Player {
    SENTE,
    GOTE;

    val opponent: Player get() = if (this == SENTE) GOTE else SENTE

    /** 盤上の「前方」の行方向。先手は上(row が減る)へ進む。 */
    val forward: Int get() = if (this == SENTE) -1 else 1
}

enum class PieceType(val kanji: String, val value: Int) {
    FU("歩", 1),
    KYO("香", 3),
    KEI("桂", 4),
    GIN("銀", 5),
    KIN("金", 6),
    KAKU("角", 8),
    HI("飛", 10),
    OU("玉", 1000),
    TO("と", 7),
    NARIKYO("杏", 6),
    NARIKEI("圭", 6),
    NARIGIN("全", 6),
    UMA("馬", 12),
    RYU("龍", 14);

    val promoted: PieceType?
        get() = when (this) {
            FU -> TO
            KYO -> NARIKYO
            KEI -> NARIKEI
            GIN -> NARIGIN
            KAKU -> UMA
            HI -> RYU
            else -> null
        }

    /** 成駒なら元の駒、そうでなければ自身。持ち駒になるときの駒種。 */
    val unpromoted: PieceType
        get() = when (this) {
            TO -> FU
            NARIKYO -> KYO
            NARIKEI -> KEI
            NARIGIN -> GIN
            UMA -> KAKU
            RYU -> HI
            else -> this
        }

    val canPromote: Boolean get() = promoted != null
    val isPromoted: Boolean get() = unpromoted != this

    companion object {
        /** 持ち駒として表示する順番。 */
        val HAND_ORDER = listOf(HI, KAKU, KIN, GIN, KEI, KYO, FU)
    }
}

data class Piece(val type: PieceType, val owner: Player)

/** 盤上のマス。row 0 が一段目(後手側)、col 0 が9筋(左端)。 */
data class Square(val row: Int, val col: Int) {
    val isOnBoard: Boolean get() = row in 0..8 && col in 0..8
    val index: Int get() = row * 9 + col

    /** 将棋の筋(1〜9)。 */
    val file: Int get() = 9 - col

    /** 将棋の段(1〜9)。 */
    val rank: Int get() = row + 1

    fun offset(dr: Int, dc: Int) = Square(row + dr, col + dc)

    override fun toString(): String = "$file$rank"

    companion object {
        val ALL: List<Square> = (0 until 81).map { Square(it / 9, it % 9) }
    }
}

/** 指し手。from が null の場合は持ち駒を打つ手。 */
data class Move(
    val from: Square?,
    val to: Square,
    val dropType: PieceType? = null,
    val promote: Boolean = false,
) {
    val isDrop: Boolean get() = from == null

    companion object {
        fun drop(type: PieceType, to: Square) = Move(null, to, type)
    }
}

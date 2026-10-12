package com.tktkcompany.shogi.online

import kotlinx.coroutines.flow.Flow

/** オンライン対戦のサーバーとのやりとり。 */
interface OnlineRepository {
    /** ログインして自分のユーザーIDを返す。 */
    suspend fun signIn(): String

    /** 部屋を作り、部屋番号を返す。 */
    suspend fun createRoom(uid: String): String

    /** 部屋に参加する。参加できなかった場合はその理由を例外で返す。 */
    suspend fun joinRoom(code: String, uid: String)

    /** 部屋の状態を監視する。部屋が存在しない間は null を流す。 */
    fun observeRoom(code: String): Flow<Room?>

    /** index 手目を送る。 */
    suspend fun sendMove(code: String, index: Int, move: RemoteMove)

    suspend fun resign(code: String, uid: String)
}

class OnlineException(message: String) : Exception(message)

/** オンライン対戦の準備状況。repository が null なら problem に使えない理由が入る。 */
data class OnlineSetup(val repository: OnlineRepository?, val problem: String?)

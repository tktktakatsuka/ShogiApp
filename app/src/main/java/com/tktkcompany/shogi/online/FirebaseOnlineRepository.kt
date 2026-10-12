package com.tktkcompany.shogi.online

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * Firebase Realtime Database を使った実装。
 *
 * データ構造:
 * ```
 * rooms/{部屋番号}/
 *   hostUid, guestUid, status("waiting"|"playing"|"finished"), createdAt, resignedBy
 *   moves/{0,1,2,...}/ { usi, by }
 * ```
 */
class FirebaseOnlineRepository private constructor() : OnlineRepository {
    private val auth = FirebaseAuth.getInstance()
    private val rooms: DatabaseReference = FirebaseDatabase.getInstance().reference.child("rooms")

    override suspend fun signIn(): String {
        auth.currentUser?.let { return it.uid }
        val user = auth.signInAnonymously().await().user
        return user?.uid ?: throw OnlineException("ログインに失敗しました")
    }

    override suspend fun createRoom(uid: String): String {
        repeat(10) {
            val code = Random.nextInt(100000, 1000000).toString()
            val (committed, _) = rooms.child(code).runTransactionAwait { data ->
                if (data.value != null) return@runTransactionAwait Transaction.abort()
                data.child("hostUid").value = uid
                data.child("status").value = "waiting"
                data.child("createdAt").value = System.currentTimeMillis()
                Transaction.success(data)
            }
            if (committed) return code
        }
        throw OnlineException("部屋を作れませんでした。もう一度お試しください")
    }

    override suspend fun joinRoom(code: String, uid: String) {
        val ref = rooms.child(code)
        if (!ref.get().await().exists()) throw OnlineException("部屋が見つかりません")
        val (_, snapshot) = ref.runTransactionAwait { data ->
            // 初回はローカルキャッシュ(null の場合あり)で呼ばれるため、変更せずに成功を返して
            // サーバーの値での再実行を待つ
            if (data.value == null) return@runTransactionAwait Transaction.success(data)
            val host = data.child("hostUid").getValue(String::class.java)
            val guest = data.child("guestUid").getValue(String::class.java)
            val status = data.child("status").getValue(String::class.java)
            if (guest != null || host == uid || status != "waiting") {
                return@runTransactionAwait Transaction.abort()
            }
            data.child("guestUid").value = uid
            data.child("status").value = "playing"
            Transaction.success(data)
        }
        when {
            snapshot == null || !snapshot.exists() -> throw OnlineException("部屋が見つかりません")
            snapshot.child("hostUid").getValue(String::class.java) == uid ->
                throw OnlineException("自分が作った部屋には参加できません")
            // 同じ端末からの再参加は guestUid が自分なのでそのまま成功扱い
            snapshot.child("guestUid").getValue(String::class.java) != uid ->
                throw OnlineException("この部屋はすでに対局中か終了しています")
        }
    }

    override fun observeRoom(code: String): Flow<Room?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toRoom(code))
            }

            override fun onCancelled(error: DatabaseError) {
                close(OnlineException("通信エラー: ${error.message}"))
            }
        }
        val ref = rooms.child(code)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun sendMove(code: String, index: Int, move: RemoteMove) {
        rooms.child(code).child("moves").child(index.toString())
            .setValue(mapOf("usi" to move.usi, "by" to move.by))
            .await()
    }

    override suspend fun resign(code: String, uid: String) {
        rooms.child(code)
            .updateChildren(mapOf("resignedBy" to uid, "status" to "finished"))
            .await()
    }

    private fun DataSnapshot.toRoom(code: String): Room? {
        val host = child("hostUid").getValue(String::class.java) ?: return null
        val moves = child("moves").children
            .sortedBy { it.key?.toIntOrNull() ?: Int.MAX_VALUE }
            .mapNotNull { m ->
                val usi = m.child("usi").getValue(String::class.java)
                val by = m.child("by").getValue(String::class.java)
                if (usi != null && by != null) RemoteMove(usi, by) else null
            }
        return Room(
            code = code,
            hostUid = host,
            guestUid = child("guestUid").getValue(String::class.java),
            status = when (child("status").getValue(String::class.java)) {
                "playing" -> RoomStatus.PLAYING
                "finished" -> RoomStatus.FINISHED
                else -> RoomStatus.WAITING
            },
            moves = moves,
            resignedBy = child("resignedBy").getValue(String::class.java),
        )
    }

    private suspend fun DatabaseReference.runTransactionAwait(
        update: (MutableData) -> Transaction.Result,
    ): Pair<Boolean, DataSnapshot?> = suspendCancellableCoroutine { cont ->
        runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result = update(currentData)

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    cont.resumeWithException(OnlineException("通信エラー: ${error.message}"))
                } else {
                    cont.resume(committed to currentData)
                }
            }
        })
    }

    companion object {
        /** google-services.json が設定されていない(Firebase 未設定)場合は null。 */
        fun createOrNull(context: Context): OnlineRepository? =
            if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseOnlineRepository()
    }
}

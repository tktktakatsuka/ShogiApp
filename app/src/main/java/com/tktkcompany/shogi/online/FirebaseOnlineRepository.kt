package com.tktkcompany.shogi.online

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.tktkcompany.shogi.R
import com.tktkcompany.shogi.UiText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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
 *   review/ { index, variation: [usi...], by }    感想戦で見ている局面
 *   chat/{自動ID}/ { by, text, at }               チャット
 * ```
 */
class FirebaseOnlineRepository private constructor(
    private val auth: FirebaseAuth,
    private val rooms: DatabaseReference,
) : OnlineRepository {

    override suspend fun signIn(): String = firebaseCall {
        auth.currentUser?.let { return@firebaseCall it.uid }
        val user = auth.signInAnonymously().await().user
        user?.uid ?: throw OnlineException(R.string.online_sign_in_failed)
    }

    override suspend fun createRoom(uid: String): String = firebaseCall {
        repeat(10) {
            val code = Random.nextInt(100000, 1000000).toString()
            val (committed, _) = rooms.child(code).runTransactionAwait { data ->
                if (data.value != null) return@runTransactionAwait Transaction.abort()
                data.child("hostUid").value = uid
                data.child("status").value = "waiting"
                data.child("createdAt").value = System.currentTimeMillis()
                Transaction.success(data)
            }
            if (committed) return@firebaseCall code
        }
        throw OnlineException(R.string.online_create_room_retry)
    }

    override suspend fun joinRoom(code: String, uid: String): Unit = firebaseCall {
        val ref = rooms.child(code)
        if (!ref.get().await().exists()) throw OnlineException(R.string.online_room_not_found)
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
            snapshot == null || !snapshot.exists() -> throw OnlineException(R.string.online_room_not_found)
            snapshot.child("hostUid").getValue(String::class.java) == uid ->
                throw OnlineException(R.string.online_own_room)
            // 同じ端末からの再参加は guestUid が自分なのでそのまま成功扱い
            snapshot.child("guestUid").getValue(String::class.java) != uid ->
                throw OnlineException(R.string.online_room_unavailable)
        }
        Unit
    }

    override fun observeRoom(code: String): Flow<Room?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toRoom(code))
            }

            override fun onCancelled(error: DatabaseError) {
                close(OnlineException(describe(error.toException())))
            }
        }
        val ref = rooms.child(code)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun sendMove(code: String, index: Int, move: RemoteMove): Unit = firebaseCall {
        rooms.child(code).child("moves").child(index.toString())
            .setValue(mapOf("usi" to move.usi, "by" to move.by))
            .await()
        Unit
    }

    override suspend fun resign(code: String, uid: String): Unit = firebaseCall {
        rooms.child(code)
            .updateChildren(mapOf("resignedBy" to uid, "status" to "finished"))
            .await()
        Unit
    }

    override suspend fun sendReview(code: String, uid: String, review: RemoteReview): Unit = firebaseCall {
        rooms.child(code).child("review")
            .setValue(mapOf("index" to review.index, "variation" to review.variation, "by" to uid))
            .await()
        Unit
    }

    override suspend fun sendChat(code: String, uid: String, text: String): Unit = firebaseCall {
        rooms.child(code).child("chat").push()
            .setValue(mapOf("by" to uid, "text" to text, "at" to System.currentTimeMillis()))
            .await()
        Unit
    }

    /**
     * Firebase の処理を時間制限付きで実行し、失敗したら原因がわかるメッセージに変換する。
     */
    private suspend fun <T> firebaseCall(block: suspend () -> T): T =
        try {
            withTimeout(TIMEOUT_MILLIS) { block() }
        } catch (e: OnlineException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            throw OnlineException(R.string.online_timeout)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OnlineException(describe(e))
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
            review = child("review").takeIf { it.exists() }?.let { r ->
                RemoteReview(
                    index = r.child("index").getValue(Int::class.java) ?: 0,
                    variation = r.child("variation").children
                        .sortedBy { it.key?.toIntOrNull() ?: Int.MAX_VALUE }
                        .mapNotNull { it.getValue(String::class.java) },
                    by = r.child("by").getValue(String::class.java),
                )
            },
            // 自動IDは時刻順に並ぶ
            chat = child("chat").children.mapNotNull { m ->
                val by = m.child("by").getValue(String::class.java)
                val text = m.child("text").getValue(String::class.java)
                val id = m.key
                if (by != null && text != null && id != null) {
                    ChatMessage(id, by, text, m.child("at").getValue(Long::class.java) ?: 0L)
                } else {
                    null
                }
            }.sortedBy { it.id },
        )
    }

    private suspend fun DatabaseReference.runTransactionAwait(
        update: (MutableData) -> Transaction.Result,
    ): Pair<Boolean, DataSnapshot?> = suspendCancellableCoroutine { cont ->
        runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result = update(currentData)

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    cont.resumeWithException(OnlineException(describe(error.toException())))
                } else {
                    cont.resume(committed to currentData)
                }
            }
        })
    }

    companion object {
        private const val TIMEOUT_MILLIS = 15_000L

        /** Firebase の設定を確認してリポジトリを作る。使えない場合はその理由を返す。 */
        fun create(context: Context): OnlineSetup {
            if (FirebaseApp.getApps(context).isEmpty()) {
                return OnlineSetup(
                    repository = null,
                    problem = UiText(R.string.online_not_initialized),
                )
            }
            return try {
                val rooms = FirebaseDatabase.getInstance().reference.child("rooms")
                OnlineSetup(FirebaseOnlineRepository(FirebaseAuth.getInstance(), rooms), problem = null)
            } catch (e: Exception) {
                OnlineSetup(
                    repository = null,
                    problem = UiText(R.string.online_no_database, e.message.orEmpty()),
                )
            }
        }

        private fun describe(e: Throwable): UiText {
            val detail = e.message.orEmpty()
            return when {
                e is FirebaseNetworkException -> UiText(R.string.online_no_internet)
                e is FirebaseAuthException && (
                    e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                        e.errorCode == "ERROR_ADMIN_RESTRICTED_OPERATION" ||
                        detail.contains("administrators", true)
                    ) -> UiText(R.string.online_anonymous_disabled)
                e is FirebaseAuthException -> UiText(R.string.online_sign_in_failed_detail, detail)
                detail.contains("permission", true) -> UiText(R.string.online_permission_denied)
                else -> UiText(R.string.online_error_detail, e.javaClass.simpleName, detail)
            }
        }
    }
}

package com.tktkcompany.shogi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.android.gms.ads.MobileAds
import com.tktkcompany.shogi.ui.Invite
import com.tktkcompany.shogi.ui.ShogiApp
import com.tktkcompany.shogi.ui.ShogiViewModel
import com.tktkcompany.shogi.ui.theme.ShogiTheme
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private val viewModel: ShogiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 広告 SDK の準備は時間がかかるので、画面の表示を待たせないよう別スレッドで行う
        thread { MobileAds.initialize(applicationContext) }
        // 画面の回転などで作り直されたときは、同じ招待で二重に参加しない
        if (savedInstanceState == null) handleInvite(intent)
        setContent {
            ShogiTheme {
                ShogiApp(viewModel)
            }
        }
    }

    /** アプリを開いたまま招待リンクから呼ばれたとき。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInvite(intent)
    }

    private fun handleInvite(intent: Intent?) {
        // 履歴(最近使ったアプリ)から開き直したときは、昔の招待で参加し直さない
        if (intent == null || (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        Invite.roomCode(intent.data)?.let(viewModel::openInvite)
    }
}

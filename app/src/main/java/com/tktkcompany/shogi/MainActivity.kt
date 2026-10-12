package com.tktkcompany.shogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import com.tktkcompany.shogi.ui.ShogiApp
import com.tktkcompany.shogi.ui.theme.ShogiTheme
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 広告 SDK の準備は時間がかかるので、画面の表示を待たせないよう別スレッドで行う
        thread { MobileAds.initialize(applicationContext) }
        setContent {
            ShogiTheme {
                ShogiApp()
            }
        }
    }
}

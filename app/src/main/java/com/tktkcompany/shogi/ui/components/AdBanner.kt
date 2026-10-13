package com.tktkcompany.shogi.ui.components

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.tktkcompany.shogi.BuildConfig
import kotlin.concurrent.thread

/** 広告 SDK(AdMob)の準備。 */
object Ads {
    /**
     * 準備が終わったか。終わる前に広告を読み込むと、準備中のスレッドとメインスレッドが
     * 互いを待ち合ってアプリが固まることがあるので、広告の読み込みはこれを待つ。
     */
    var ready by mutableStateOf(false)
        private set

    /** 準備は時間がかかるので、画面の表示を待たせないよう別スレッドで行う。 */
    fun initialize(context: Context) {
        thread {
            MobileAds.initialize(context)
            ready = true
        }
    }
}

/**
 * バナー広告(AdMob)。置いた場所の幅に合わせた大きさで表示する。
 * 読み込み中や広告がないときも同じ高さを空けておくので、表示されたときに画面がずれない。
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val size = remember(maxWidth) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, maxWidth.value.toInt())
        }
        val adView = remember(size) {
            AdView(context).apply {
                setAdSize(size)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
            }
        }
        val ready = Ads.ready
        LaunchedEffect(adView, ready) {
            if (ready) adView.loadAd(AdRequest.Builder().build())
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(size.height.dp))
    }
}

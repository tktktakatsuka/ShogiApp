package com.tktkcompany.shogi.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.tktkcompany.shogi.BuildConfig

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
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(size.height.dp))
    }
}

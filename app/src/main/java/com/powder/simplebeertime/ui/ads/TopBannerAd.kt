package com.powder.simplebeertime.ui.ads

import androidx.compose.foundation.layout.Box
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

@Composable
fun TopBannerAd(
    isAdFree: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111" // Test Banner Ad Unit ID
) {
    // ✅ 広告の有無に関わらず高さを固定で確保（SSmTと同じ仕様）
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (!isAdFree) {
            val context = LocalContext.current

            // 再コンポーズで毎回作り直さない
            val adView = remember {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                }
            }

            // 初回だけロード、離脱時に破棄
            DisposableEffect(Unit) {
                adView.loadAd(AdRequest.Builder().build())
                onDispose { adView.destroy() }
            }

            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
        // isAdFree のときは何も描画しない＝空席だけ残る
    }
}
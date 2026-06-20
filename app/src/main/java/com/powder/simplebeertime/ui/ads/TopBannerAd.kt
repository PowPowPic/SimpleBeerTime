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
    showAds: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-7305983073191908/3591022221"
) {
    // Keep the existing 50dp banner area while consent is being resolved or ads are off.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (showAds) {
            val context = LocalContext.current
            val adView = remember(context, adUnitId) {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                }
            }

            DisposableEffect(adView) {
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
    }
}

package com.powder.simplebeertime.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    // Interstitial Ad ID
    private const val AD_UNIT_ID = "ca-app-pub-7305983073191908/2277940552"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    @Volatile
    private var adsEnabled = false

    /** Apply the shared UMP, SDK-initialization, and ad-removal gate. */
    fun setAdsEnabled(context: Context, enabled: Boolean) {
        adsEnabled = enabled
        if (enabled) {
            loadAd(context.applicationContext)
        } else {
            interstitialAd = null
            isLoading = false
        }
    }

    fun loadAd(context: Context) {
        if (!adsEnabled || interstitialAd != null || isLoading) return

        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = if (adsEnabled) ad else null
                }
            }
        )
    }

    /**
     * @param onAdClosed Called after the ad closes or when it cannot be displayed.
     * @param onAdShown Called only when the interstitial was actually shown.
     */
    fun showInterstitial(
        activity: Activity,
        onAdClosed: () -> Unit,
        onAdShown: () -> Unit
    ) {
        if (!adsEnabled) {
            onAdClosed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            loadAd(activity.applicationContext)
            onAdClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onAdShown()
                loadAd(activity.applicationContext)
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadAd(activity.applicationContext)
                onAdClosed()
            }
        }

        ad.show(activity)
    }
}

package com.powder.simplebeertime.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    // Test Interstitial Ad ID
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }
            }
        )
    }

    /**
     * @param onAdClosed 広告が閉じられた / 表示できなかった後に必ず呼ばれる
     * @param onAdShown  広告が実際に表示され、ユーザーが閉じた場合のみ呼ばれる
     */
    fun showInterstitial(
        activity: Activity,
        onAdClosed: () -> Unit,
        onAdShown: () -> Unit
    ) {
        val ad = interstitialAd

        // --- 広告がまだ無い場合 ---
        if (ad == null) {
            // 次回に備えてロードだけはしておく
            loadAd(activity)
            onAdClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                // 表示成功 → ユーザーが閉じた
                interstitialAd = null
                onAdShown()          // ★ここで6時間枠を消費
                loadAd(activity)     // 次を準備
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                // 表示失敗 → 枠は消費しない
                interstitialAd = null
                loadAd(activity)     // 次を準備
                onAdClosed()
            }
        }

        ad.show(activity)
    }
}

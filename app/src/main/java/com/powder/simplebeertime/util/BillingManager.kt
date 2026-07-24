package com.powder.simplebeertime.util

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.powder.simplebeertime.data.preferences.AdPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 既存の広告削除購入者の権利確認・復元専用BillingManager。
 *
 * 新規購入の商品取得・価格取得・購入フローは撤去済み。
 * 製品ID "remove_ads" は過去の購入履歴との照合に必要なため維持する。
 */
class BillingManager(
    context: Context,
    private val adRepository: AdPreferencesRepository
) {
    companion object {
        const val PRODUCT_ID = "remove_ads"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isConnecting = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach(::handlePurchase)
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryExistingPurchases()
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        connectAndQueryPurchases()
    }

    private fun connectAndQueryPurchases() {
        if (billingClient.isReady) {
            queryExistingPurchases()
            return
        }
        if (isConnecting) return

        isConnecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                // enableAutoServiceReconnection() により次回API呼び出し時に再接続される。
            }
        })
    }

    private fun queryExistingPurchases() {
        if (!billingClient.isReady) {
            connectAndQueryPurchases()
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                return@queryPurchasesAsync
            }

            val activePurchases = purchases.filter { purchase ->
                purchase.products.contains(PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            if (activePurchases.isEmpty()) {
                setAdFree(false)
            } else {
                activePurchases.forEach(::handlePurchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PRODUCT_ID)) return

        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    setAdFree(true)
                }
            }
        } else {
            setAdFree(true)
        }
    }

    private fun setAdFree(enabled: Boolean) {
        scope.launch {
            adRepository.setAdFree(enabled)
        }
    }

    /** アプリ復帰時などに、購入・返金・取消後の状態をGoogle Playから再確認する。 */
    fun refreshPurchases() {
        if (billingClient.isReady) {
            queryExistingPurchases()
        } else {
            connectAndQueryPurchases()
        }
    }
}

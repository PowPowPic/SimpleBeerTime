package com.powder.simplebeertime.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.powder.simplebeertime.data.preferences.AdPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Google Play In-App Billing マネージャー（広告削除専用）
 *
 * - 製品ID: "remove_ads"（Google Play Console で登録）
 * - 購入成功時に AdPreferencesRepository.setAdFree(true) を呼び出す
 * - SettingsDialogからlaunchBillingFlowを呼ぶ際は必ず findActivity() で Activity を取得すること
 *   （LocalContext.current as Activity は ContextThemeWrapper のためクラッシュする）
 */
class BillingManager(
    private val context: Context,
    private val adRepository: AdPreferencesRepository
) {
    companion object {
        const val PRODUCT_ID = "remove_ads"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 購入ボタンに表示する価格文字列（例: "¥370"）。null = まだ取得中 */
    private val _formattedPrice = MutableStateFlow<String?>(null)
    val formattedPrice: StateFlow<String?> = _formattedPrice

    private var productDetails: ProductDetails? = null
    private var isConnecting = false

    // ── PurchasesUpdatedListener ──────────────────────────────────────────────
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

    // ── BillingClient ─────────────────────────────────────────────────────────
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
        connectAndQuery()
    }

    private fun connectAndQuery() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryExistingPurchases()
            return
        }
        if (isConnecting) return

        isConnecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                // enableAutoServiceReconnection() により、次回API呼び出し時に自動再接続される。
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                return@queryProductDetailsAsync
            }

            val details = queryResult.productDetailsList.firstOrNull()
            productDetails = details
            _formattedPrice.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    private fun queryExistingPurchases() {
        if (!billingClient.isReady) {
            connectAndQuery()
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
            connectAndQuery()
        }
    }

    // ── 購入フロー起動 ────────────────────────────────────────────────────────
    fun launchBillingFlow(activity: Activity) {
        if (!billingClient.isReady) {
            connectAndQuery()
            return
        }
        val details = productDetails ?: run {
            queryProductDetails()
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        when (result.responseCode) {
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryExistingPurchases()
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> connectAndQuery()
        }
    }
}

// ── Context 拡張: Activity を安全に取得 ────────────────────────────────────────
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

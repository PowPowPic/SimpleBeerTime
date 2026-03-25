package com.powder.simplebeertime.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
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

    // ── PurchasesUpdatedListener ──────────────────────────────────────────────
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
        // ITEM_ALREADY_OWNED: ユーザーが既に購入済みの場合も確認
        if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryExistingPurchases()
        }
    }

    // ── BillingClient ─────────────────────────────────────────────────────────
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    // ── 初期化 ────────────────────────────────────────────────────────────────
    init {
        connectAndQuery()
    }

    private fun connectAndQuery() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // 再接続はシステムが自動で行う
            }
        })
    }

    // ── 製品詳細（価格）取得 ──────────────────────────────────────────────────
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

        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            productDetailsList.firstOrNull()?.let { details ->
                productDetails = details
                // 価格文字列（例: "¥370"）を抽出して公開
                _formattedPrice.value =
                    details.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    // ── 既存購入チェック（アプリ再起動後の復元） ──────────────────────────────
    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, purchases ->
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    // ── 購入処理 ──────────────────────────────────────────────────────────────
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PRODUCT_ID)) return

        // 未 Acknowledge の場合は Acknowledge する
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    grantAdFree()
                }
            }
        } else {
            // 既に Acknowledge 済み → 広告削除を付与
            grantAdFree()
        }
    }

    private fun grantAdFree() {
        scope.launch {
            adRepository.setAdFree(true)
        }
    }

    // ── 購入フロー起動 ────────────────────────────────────────────────────────
    /**
     * SettingsDialog などのダイアログから呼ぶ場合は必ず findActivity() で取得した Activity を渡すこと。
     * LocalContext.current as Activity は ContextThemeWrapper でクラッシュする。
     */
    fun launchBillingFlow(activity: Activity) {
        val details = productDetails ?: run {
            // 製品詳細未取得の場合は再取得して終了
            if (billingClient.isReady) queryProductDetails()
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

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }
}

// ── Context 拡張: Activity を安全に取得 ────────────────────────────────────────
/**
 * ダイアログ内では LocalContext.current が ContextThemeWrapper のため
 * as Activity キャストがクラッシュする。
 * この関数で Activity を辿って安全に取得する。
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

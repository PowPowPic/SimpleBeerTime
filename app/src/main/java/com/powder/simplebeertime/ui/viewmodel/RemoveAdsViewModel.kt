package com.powder.simplebeertime.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.powder.simplebeertime.util.BillingManager
import kotlinx.coroutines.flow.StateFlow

/**
 * 広告削除購入 ViewModel
 *
 * - formattedPrice: 購入ボタンに表示する価格文字列（例: "¥370"）
 * - launchBillingFlow: 購入フローを起動する
 *   ★ SettingsDialog から呼ぶ場合は context.findActivity() で Activity を取得して渡すこと
 */
class RemoveAdsViewModel(private val billingManager: BillingManager) : ViewModel() {

    /** Google Play から取得した価格文字列（ボタンラベルに表示） */
    val formattedPrice: StateFlow<String?> = billingManager.formattedPrice

    /** 購入フローを起動する */
    fun launchBillingFlow(activity: Activity) {
        billingManager.launchBillingFlow(activity)
    }
}

class RemoveAdsViewModelFactory(
    private val billingManager: BillingManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemoveAdsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RemoveAdsViewModel(billingManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

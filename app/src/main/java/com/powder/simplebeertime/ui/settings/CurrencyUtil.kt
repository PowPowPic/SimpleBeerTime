package com.powder.simplebeertime.ui.settings

import java.util.Currency
import java.util.Locale

fun currencySymbolFor(lang: AppLanguage): String {
    return when (lang) {
        AppLanguage.SYSTEM -> {
            // 端末の地域から通貨を推定
            val locale = Locale.getDefault()
            runCatching { Currency.getInstance(locale).symbol }
                .getOrElse { "$" } // フォールバック
        }
        AppLanguage.ENGLISH -> "$"
        AppLanguage.JAPANESE -> "¥"
        AppLanguage.FRENCH -> "€"
        AppLanguage.GERMAN -> "€"
        AppLanguage.SPANISH -> "€"
        AppLanguage.ITALIAN -> "€"
        AppLanguage.PORTUGUESE_BR -> "R$"
        AppLanguage.INDONESIAN -> "Rp"
        AppLanguage.THAI -> "฿"
        AppLanguage.TURKISH -> "₺"
        AppLanguage.VIETNAMESE -> "₫"
        AppLanguage.CHINESE_TRADITIONAL -> "NT$"
        AppLanguage.KOREAN -> "₩"
        AppLanguage.ARABIC -> "ج.م"
    }
}
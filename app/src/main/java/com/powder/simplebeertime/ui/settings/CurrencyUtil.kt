package com.powder.simplebeertime.ui.settings

import java.util.Currency
import java.util.Locale

/**
 * 言語設定から通貨記号を取得
 */
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

/**
 * 言語設定から通貨コードを取得
 */
fun currencyCodeFor(lang: AppLanguage): String {
    return when (lang) {
        AppLanguage.SYSTEM -> {
            val locale = Locale.getDefault()
            runCatching { Currency.getInstance(locale).currencyCode }
                .getOrElse { "USD" }
        }
        AppLanguage.ENGLISH -> "USD"
        AppLanguage.JAPANESE -> "JPY"
        AppLanguage.FRENCH -> "EUR"
        AppLanguage.GERMAN -> "EUR"
        AppLanguage.SPANISH -> "EUR"
        AppLanguage.ITALIAN -> "EUR"
        AppLanguage.PORTUGUESE_BR -> "BRL"
        AppLanguage.INDONESIAN -> "IDR"
        AppLanguage.THAI -> "THB"
        AppLanguage.TURKISH -> "TRY"
        AppLanguage.VIETNAMESE -> "VND"
        AppLanguage.CHINESE_TRADITIONAL -> "TWD"
        AppLanguage.KOREAN -> "KRW"
        AppLanguage.ARABIC -> "EGP"
    }
}

/**
 * 通貨の小数桁数を取得（JPY=0, USD=2 など）
 */
fun getCurrencyFractionDigits(lang: AppLanguage): Int {
    val currencyCode = currencyCodeFor(lang)
    return try {
        Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
}

/**
 * 金額を通貨に応じたフォーマットで表示（基本用）
 * JPY/KRW/VND → 整数表示（¥200）
 * USD/EUR → 小数2桁表示（$2.88）
 */
fun formatCurrencyAmount(lang: AppLanguage, symbol: String, amount: Double): String {
    val fractionDigits = getCurrencyFractionDigits(lang)
    return if (fractionDigits == 0) {
        "$symbol${amount.toLong()}"
    } else {
        "$symbol${String.format(Locale.US, "%.${fractionDigits}f", amount)}"
    }
}
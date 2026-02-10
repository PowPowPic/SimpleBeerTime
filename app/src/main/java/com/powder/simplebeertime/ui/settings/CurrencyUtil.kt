package com.powder.simplebeertime.ui.settings

import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

/**
 * 言語設定から通貨記号を取得
 */
fun currencySymbolFor(lang: AppLanguage): String {
    return when (lang) {
        AppLanguage.SYSTEM -> {
            val locale = Locale.getDefault()
            runCatching { Currency.getInstance(locale).symbol }
                .getOrElse { "$" }
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
 * ★ 通貨の小数表示ルール（確定版）
 *
 * 小数を表示する言語：en, de, fr, es, it, ar → 0〜2桁（ロケール準拠）
 * 常に整数表示の言語：ja, ko, zh-TW, id, th, vi, tr, pt-BR → 四捨五入で整数
 */
fun isDecimalCurrency(lang: AppLanguage): Boolean {
    return when (lang) {
        // ★ 小数表示する言語
        AppLanguage.ENGLISH,
        AppLanguage.GERMAN,
        AppLanguage.FRENCH,
        AppLanguage.SPANISH,
        AppLanguage.ITALIAN,
        AppLanguage.ARABIC -> true

        // ★ 常に整数表示の言語
        AppLanguage.JAPANESE,
        AppLanguage.KOREAN,
        AppLanguage.CHINESE_TRADITIONAL,
        AppLanguage.INDONESIAN,
        AppLanguage.THAI,
        AppLanguage.VIETNAMESE,
        AppLanguage.TURKISH,
        AppLanguage.PORTUGUESE_BR -> false

        // SYSTEM: 端末のロケールから推定
        AppLanguage.SYSTEM -> {
            val systemLang = Locale.getDefault().language
            systemLang in listOf("en", "de", "fr", "es", "it", "ar")
        }
    }
}

/**
 * ★ 金額を通貨に応じたフォーマットで表示
 *
 * 小数表示言語（en, de, fr, es, it, ar）：
 *   → ロケール準拠で0〜2桁の小数（例：$127.50, €2.88）
 *
 * 整数表示言語（ja, ko, zh-TW, id, th, vi, tr, pt-BR）：
 *   → 常に整数（四捨五入）（例：¥128, Rp125000, ₩3000）
 */
fun formatCurrencyAmount(lang: AppLanguage, symbol: String, amount: Double): String {
    return if (isDecimalCurrency(lang)) {
        // 小数表示言語：2桁固定
        "$symbol${String.format(Locale.US, "%.2f", amount)}"
    } else {
        // 整数表示言語：四捨五入
        "$symbol${amount.roundToLong()}"
    }
}

/**
 * ★ 本数のスマートフォーマット（全画面共通）
 * 整数なら小数点なし（3）、小数ありなら小数1桁（1.4）
 */
fun formatBeerCount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

/**
 * ★ 1日あたりの平均金額フォーマット
 * 通貨フォーマット + "/day"サフィックス
 */
fun formatCurrencyPerDay(lang: AppLanguage, symbol: String, amount: Double, perDaySuffix: String): String {
    val formatted = formatCurrencyAmount(lang, symbol, amount)
    return "$formatted $perDaySuffix"
}
package com.powder.simplebeertime.ui.settings

import java.util.Currency
import java.util.Locale

/**
 * 言語設定から通貨記号を取得
 */
fun currencySymbolFor(lang: AppLanguage): String {
    return when (lang) {
        AppLanguage.SYSTEM -> {
            val locale = Locale.getDefault()
            val code = runCatching { Currency.getInstance(locale).currencyCode }.getOrElse { "" }
            // ★ SGD は java.util.Currency が "$" を返す端末があるためハードコード
            if (code == "SGD") return "S$"
            runCatching { Currency.getInstance(locale).getSymbol(locale) }
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
 * 言語設定からLocaleを取得
 * ★ ロケール依存フォーマット（桁区切り・小数点記号）に必要
 */
fun localeFor(lang: AppLanguage): Locale {
    return when (lang) {
        AppLanguage.SYSTEM -> {
            val locale = Locale.getDefault()
            // ★ en-ZA: ICU/CLDR の公式規格は小数点","・桁区切り" "だが
            //   実生活（スーパー・銀行明細）では小数点"."・桁区切り","が標準。
            //   ユーザー体験に合わせて en-US フォーマット（. と ,）を強制適用。
            if (locale.country == "ZA") Locale.US else locale
        }
        AppLanguage.ENGLISH -> Locale.US
        AppLanguage.JAPANESE -> Locale.JAPAN
        AppLanguage.FRENCH -> Locale.FRANCE
        AppLanguage.GERMAN -> Locale.GERMANY
        AppLanguage.SPANISH -> Locale("es", "ES")
        AppLanguage.ITALIAN -> Locale.ITALY
        AppLanguage.PORTUGUESE_BR -> Locale("pt", "BR")
        AppLanguage.INDONESIAN -> Locale("in", "ID")
        AppLanguage.THAI -> Locale("th", "TH")
        AppLanguage.TURKISH -> Locale("tr", "TR")
        AppLanguage.VIETNAMESE -> Locale("vi", "VN")
        AppLanguage.CHINESE_TRADITIONAL -> Locale("zh", "TW")
        AppLanguage.KOREAN -> Locale.KOREA
        AppLanguage.ARABIC -> Locale("ar", "EG")
    }
}

/**
 * ★ 通貨の実用小数桁数を取得
 *
 * 基本はCurrency.defaultFractionDigitsに従うが、
 * 実生活で整数しか使わない通貨は手動で0にオーバーライドする。
 *
 * IDR: 税務上は小数2桁だが、実生活ではRp100が最小単位 → 整数扱い
 */
fun getCurrencyFractionDigits(lang: AppLanguage): Int {
    val currencyCode = currencyCodeFor(lang)

    // ★ 実生活で整数のみの通貨をオーバーライド
    //   IDR: 実生活では Rp1,000 が最小単位（小数なし）
    //   VND: ₫1,000 が最小単位（小数なし）
    val integerOverrides = setOf("IDR", "VND")
    if (currencyCode in integerOverrides) return 0

    return try {
        Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
}

// 通貨記号を数値の後に置く通貨コードセット（後置き）
//   Android の NumberFormat は端末バージョンによって記号位置が不安定なため
//   既知の後置き通貨はハードコードで管理する
private val POSTFIX_CURRENCIES = setOf(
    "EUR",   // ユーロ（de/fr/it/es）→ 17,88 €
    "VND",   // ベトナムドン        → 1.234 ₫
    "TRY",   // トルコリラ          → 1.234,56 ₺
)

/**
 * ★ 金額を通貨に応じたフォーマットで表示（ロケール依存・桁区切りあり）
 *
 * 小数桁数：getCurrencyFractionDigits()で判定
 *   JPY(0), KRW(0), VND(0), IDR(0:オーバーライド) → 整数表示
 *   USD(2), EUR(2), BRL(2), THB(2), TRY(2), TWD(2), EGP(2) → 小数2桁表示
 *
 * 小数点記号：ロケールで自動判定
 *   en, ar, th, zh-TW → "."（ドット）
 *   de, fr, es, it, pt-BR, tr → ","（カンマ）
 *
 * 桁区切り：ロケールで自動判定
 *   en, ja, ko, th, zh-TW, ar → ","（カンマ）
 *   de, es, it, pt-BR, id, tr, vi → "."（ドット）
 *   fr → " "（空白）
 *
 * 記号位置：POSTFIX_CURRENCIES に従い後置き対応
 *   en: $125,000.50    de: 125.000,50 €    fr: 125 000,50 €
 *   ja: ¥125,000       ko: ₩125,000        id: Rp125.000
 *   th: ฿125,000.50    tr: 125.000,50 ₺    vi: 125.000 ₫
 */
fun formatCurrencyAmount(lang: AppLanguage, symbol: String, amount: Double): String {
    val fractionDigits = getCurrencyFractionDigits(lang)
    val locale = localeFor(lang)
    val number = if (fractionDigits == 0) {
        String.format(locale, "%,d", Math.round(amount))
    } else {
        String.format(locale, "%,.${fractionDigits}f", amount)
    }
    val currencyCode = currencyCodeFor(lang)
    return if (currencyCode in POSTFIX_CURRENCIES) {
        "$number $symbol"   // 例: 17,88 €
    } else {
        "$symbol$number"    // 例: $125,000.50
    }
}

/**
 * ★ 本数のスマートフォーマット（全画面共通）
 * 整数なら小数点なし（3）、小数ありなら小数1桁（1.4 / 1,4）
 * ★ ロケール準拠: 小数点記号はロケールに従う（de/fr/es/it等 → カンマ）
 * lang省略時はシステムロケールを使用
 */
fun formatBeerCount(value: Double, lang: AppLanguage? = null): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        val locale = run {
            val l = if (lang != null) localeFor(lang) else Locale.getDefault()
            // ★ en-ZA: 実生活慣習に合わせて小数点"."を強制
            if (l.country == "ZA") Locale.US else l
        }
        String.format(locale, "%.1f", value)
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
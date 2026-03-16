package com.powder.simplebeertime.ui.settings

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────
// 設計方針（移行後）
//
// MainActivity.attachBaseContext() が Locale.setDefault() を選択言語で
// 設定済みのため、通貨ロジックはすべて Locale.getDefault() ベースで自動処理。
// Java/Android が正しく返せない部分のみオーバーライドで対応。
//
// 新言語追加時に必要な作業:
//   1. AppLanguage.kt に enum 追加
//   2. MainActivity.tagToLocale() に追加
//   3. LanguageSettingDialog に追加
//   4. 必要なオーバーライドがあればここに追加（通常は不要）
// ─────────────────────────────────────────────────────────────────────────

/**
 * 現在有効なロケールを取得
 * attachBaseContext が設定した Locale.getDefault() を正規化して返す
 */
fun getCurrentLocale(): Locale {
    return normalizeLocaleForCurrency(Locale.getDefault())
}

/**
 * 国コードなしロケール（例: Locale("tr")）を通貨フォーマット用に正規化
 */
private fun normalizeLocaleForCurrency(locale: Locale): Locale {
    if (locale.country.isNotEmpty()) return locale
    return when (locale.language) {
        "ja"       -> Locale("ja", "JP")
        "en"       -> Locale("en", "US")
        "es"       -> Locale("es", "ES")
        "it"       -> Locale("it", "IT")
        "pt"       -> Locale("pt", "BR")
        "fr"       -> Locale("fr", "FR")
        "de"       -> Locale("de", "DE")
        "ar"       -> Locale("ar", "EG")
        "in"       -> Locale("in", "ID")
        "th"       -> Locale("th", "TH")
        "tr"       -> Locale("tr", "TR")
        "vi"       -> Locale("vi", "VN")
        "zh"       -> Locale("zh", "TW")
        "ko"       -> Locale("ko", "KR")
        "pl"       -> Locale("pl", "PL")
        "ro"       -> Locale("ro", "RO")
        "uz"       -> Locale("uz", "UZ")
        "kk"       -> Locale("kk", "KZ")
        "ur"       -> Locale("ur", "PK")
        "ky"       -> Locale("ky", "KG")
        "bg"       -> Locale("bg", "BG")
        "az"       -> Locale("az", "AZ")
        else       -> locale
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 実生活で整数のみの通貨
// ─────────────────────────────────────────────────────────────────────────
private val INTEGER_ONLY_CURRENCIES = setOf(
    "IDR", "VND", "KRW", "JPY", "UZS", "KZT", "KGS",
)

// ─────────────────────────────────────────────────────────────────────────
// 後置き通貨（Android バージョンによって前置きで返す端末への対策）
// ─────────────────────────────────────────────────────────────────────────
private val POSTFIX_CURRENCIES = setOf(
    "EUR",  // ユーロ（de/fr/it/es/pt-BR + bg 2026年2月より）
    "TRY",  // トルコリラ → "1,00 ₺"
    "VND",  // ベトナムドン → "1 ₫"
    "PLN",  // ポーランドズウォティ → "100 zł"
    "RON",  // ルーマニアレウ → "1 234,56 lei"
    "UZS",  // ウズベクスム → "1 000 soʻm"
    "KZT",  // カザフテンゲ → "1 000 ₸"
    "KGS",  // キルギスソム → "100 сом"
    "AZN",  // アゼルバイジャンマナト → "100,00 ₼"
)

/**
 * 現在の通貨コードを取得
 */
fun getSelectedCurrencyCode(): String {
    val locale = getCurrentLocale()
    // ★ BGN→EUR オーバーライド（ブルガリア 2026年2月よりユーロ移行）
    if (locale.language == "bg" || locale.country == "BG") return "EUR"
    return try {
        Currency.getInstance(locale)?.currencyCode ?: "USD"
    } catch (_: Exception) {
        currencyCodeFromLanguage(locale.language)
    }
}

private fun currencyCodeFromLanguage(lang: String): String = when (lang) {
    "ja"       -> "JPY"
    "ko"       -> "KRW"
    "zh"       -> "TWD"
    "th"       -> "THB"
    "vi"       -> "VND"
    "in", "id" -> "IDR"
    "de", "fr", "it", "es" -> "EUR"
    "pt"       -> "BRL"
    "tr"       -> "TRY"
    "ar"       -> "EGP"
    "en"       -> "USD"
    "pl"       -> "PLN"
    "ro"       -> "RON"
    "uz"       -> "UZS"
    "kk"       -> "KZT"
    "ur"       -> "PKR"
    "ky"       -> "KGS"
    "bg"       -> "EUR"
    "az"       -> "AZN"
    else       -> "USD"
}

/**
 * 通貨の実用小数桁数を取得
 */
fun getCurrencyFractionDigits(): Int {
    val code = getSelectedCurrencyCode()
    if (code in INTEGER_ONLY_CURRENCIES) return 0
    return try {
        Currency.getInstance(code).defaultFractionDigits.coerceAtLeast(0)
    } catch (_: Exception) {
        2
    }
}

/**
 * 通貨記号を取得
 *
 * ★ オーバーライドが必要な通貨:
 *   BGN→€  : ブルガリア 2026年2月よりユーロ移行
 *   RON→lei : "RON"（銀行表記）ではなく日常使いの "lei"
 *   PKR→Rs  : "₨" ではなく "Rs " スペース付き（実生活慣習）
 *   KGS→сом : キリル文字（Java は "KGS" や "som" を返す）
 *   ZAR→R   : en-ZA で "$" にフォールバックする端末対策
 *   SGD→S$  : Java は "$" を返すことがある
 *   MXN→MX$ : en-US の "$" と区別
 *   EGP→ج.م : スペース付き
 */
fun getCurrencySymbol(): String {
    val locale = getCurrentLocale()

    // ★ BGN→EUR
    if (locale.language == "bg" || locale.country == "BG") return "€"
    // ★ RON→lei
    if (locale.language == "ro" || locale.country == "RO") return "lei"
    // ★ PKR→Rs （半角スペース付き）
    if (locale.language == "ur" || locale.country == "PK") return "Rs "
    // ★ KGS→сом
    if (locale.language == "ky" || locale.country == "KG") return "сом"
    // ★ AZN→₼（古い端末で "AZN" や "man." が返る場合の対策）
    if (locale.language == "az" || locale.country == "AZ") return "₼"
    // ★ ZAR→R
    if (locale.country == "ZA") return "R"
    // ★ MXN→MX$
    if (locale.country == "MX") return "MX$"

    val code = try { Currency.getInstance(locale)?.currencyCode } catch (_: Exception) { null }
    // ★ SGD→S$
    if (code == "SGD") return "S$"
    // ★ TWD→NT$（Java は "$" を返す端末がある）
    if (code == "TWD") return "NT$"
    // ★ EGP→ج.م
    if (code == "EGP") return "ج.م "

    val symbolFromLocale = try {
        Currency.getInstance(locale)?.getSymbol(locale)
    } catch (_: Exception) { null }

    if (!symbolFromLocale.isNullOrEmpty() && locale.country.isNotEmpty()) {
        return symbolFromLocale
    }

    return when (locale.language) {
        "ja"       -> "¥"
        "ko"       -> "₩"
        "zh"       -> "NT$"
        "th"       -> "฿"
        "vi"       -> "₫"
        "in", "id" -> "Rp"
        "de", "fr", "it", "es", "bg" -> "€"
        "pt"       -> "R$"
        "tr"       -> "₺"
        "ar"       -> "ج.م "
        "en"       -> "$"
        "pl"       -> "zł"
        "ro"       -> "lei"
        "uz"       -> "soʻm"
        "kk"       -> "₸"
        "ur"       -> "Rs "
        "ky"       -> "сом"
        "az"       -> "₼"
        else       -> symbolFromLocale ?: "$"
    }
}

/**
 * ★ 金額を通貨形式でフォーマット
 *
 * Locale.getDefault() に委任し、正しく動かない部分のみオーバーライド:
 *   ルーマニア : NNBSP 桁区切り（U+202F）強制
 *   PKR       : 端末依存記号を "Rs " に統一
 *   後置き通貨 : 前置きで返す端末への対策
 *   EGP/PKR   : RTL BiDi 崩れ防止（LRI/PDI wrap）
 */
fun formatCurrencyAmount(amount: Double): String {
    val locale = getCurrentLocale()
    val code = getSelectedCurrencyCode()
    val symbol = getCurrencySymbol()
    val fractionDigits = getCurrencyFractionDigits()

    // ★ ルーマニア: NNBSP 桁区切り強制 → "1 234,56 lei"
    if (locale.language == "ro" || locale.country == "RO") {
        val symbols = DecimalFormatSymbols(locale).also {
            it.groupingSeparator = '\u202F' // NARROW NO-BREAK SPACE
        }
        val pattern = if (fractionDigits > 0) "#,##0.${"0".repeat(fractionDigits)}" else "#,##0"
        val df = DecimalFormat(pattern, symbols)
        return "${df.format(amount)} $symbol"
    }

    val fmt = buildFormatter(locale, code, fractionDigits)
    val result = fmt.format(amount)

    // ★ PKR: "Rs 1,234.56" に統一 + BiDi wrap
    if (code == "PKR") {
        val fmtSymbol = try { Currency.getInstance(code).getSymbol(locale) } catch (_: Exception) { "" }
        val numberPart = result
            .replace(fmtSymbol, "")
            .replace(symbol.trim(), "")
            .replace(code, "")
            .trim()
        return bidiWrap("$symbol$numberPart", locale)
    }

    // ★ 後置き通貨
    if (code in POSTFIX_CURRENCIES) {
        val numberPart = result.replace(symbol.trim(), "").replace(code, "").trim()
        return "$numberPart $symbol"
    }

    // ★ EGP + BiDi wrap
    if (code == "EGP") {
        val fmtSymbol = try { Currency.getInstance(code).getSymbol(locale) } catch (_: Exception) { "" }
        val numberPart = result
            .replace(fmtSymbol, "")
            .replace(symbol.trim(), "")
            .replace(code, "")
            .trim()
        return bidiWrap("$symbol$numberPart", locale)
    }

    // ★ SGD
    if (code == "SGD") return result.replace("$", "S$")
    // ★ TWD: Java が "$" を返す端末で "NT$" に置換
    if (code == "TWD") return result.replace("$", "NT$")

    return bidiWrap(result, locale)
}

/**
 * 通貨フォーマット + "/day" サフィックス
 */
fun formatCurrencyPerDay(amount: Double, perDaySuffix: String): String {
    return "${formatCurrencyAmount(amount)} $perDaySuffix"
}

/**
 * ★ 本数のスマートフォーマット
 * 整数なら小数点なし（3）、小数ありなら小数1桁（1.4 / 1,4）
 * ロケールの小数点記号に従う（de/fr/es等 → カンマ）
 */
fun formatBeerCount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        val locale = getCurrentLocale().let {
            // ★ en-ZA: 実生活慣習に合わせて小数点 "." を強制
            if (it.country == "ZA") Locale.US else it
        }
        String.format(locale, "%.1f", value)
    }
}

/**
 * RTL ロケール内の BiDi 崩れ防止（LRI/PDI wrap）
 */
private fun bidiWrap(text: String, locale: Locale): String {
    return if (locale.language in setOf("ur", "ar", "fa", "he", "yi")) {
        "\u2066$text\u2069"
    } else {
        text
    }
}

private fun buildFormatter(locale: Locale, code: String, fractionDigits: Int): NumberFormat {
    val fmt = NumberFormat.getCurrencyInstance(locale)
    try { fmt.currency = Currency.getInstance(code) } catch (_: Exception) {}
    if (code in INTEGER_ONLY_CURRENCIES) {
        fmt.maximumFractionDigits = 0
        fmt.minimumFractionDigits = 0
    }
    return fmt
}
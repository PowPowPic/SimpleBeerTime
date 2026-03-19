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
// ★ 整数通貨だが1単価に小数点第1位が有効な通貨
//   タバコ値上げ・ビール価格端数等で1単価が小数になるケースに対応
//   （IDR/VND/UZS は桁が大きいため対象外）
// ─────────────────────────────────────────────────────────────────────────
private val DECIMAL1_INPUT_CURRENCIES = setOf(
    "JPY",  // 日本円（例: 26.5円/本）
    "KRW",  // 韓国ウォン（例: 225.5₩/本）
    "TWD",  // 台湾ドル（例: 7.5 NT$/本）
    "KZT",  // カザフテンゲ（例: 50.5₸/本）
    "KGS",  // キルギスソム（例: 5.5 сом/本）
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
 * ★ 1単価入力で許可する最大小数桁数
 *   DECIMAL1対象通貨 → 1（小数点第1位まで）
 *   その他の整数通貨(IDR/VND/UZS) → 0（整数のみ）
 *   小数点通貨(USD/EUR等) → 2（従来通り）
 */
fun getInputMaxDecimalPlaces(): Int {
    val code = getSelectedCurrencyCode()
    if (code in DECIMAL1_INPUT_CURRENCIES) return 1
    return getCurrencyFractionDigits()  // 0 for IDR/VND/UZS, 2 for USD/EUR etc.
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
 * ★ 金額フォーマットの内部共通処理
 *
 * fractionDigits を外部から指定できるようにし、
 * formatCurrencyAmount / formatCurrencyAmountSmart / formatCurrencyAmountForAverage
 * から共通で呼び出す。
 */
private fun formatCurrencyAmountInternal(amount: Double, fractionDigits: Int): String {
    val locale = getCurrentLocale()
    val code = getSelectedCurrencyCode()
    val symbol = getCurrencySymbol()

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
 * ★ 金額を通貨形式でフォーマット（従来互換）
 *
 * 整数通貨は小数なし、小数通貨は既定桁数で表示。
 */
fun formatCurrencyAmount(amount: Double): String {
    return formatCurrencyAmountInternal(amount, getCurrencyFractionDigits())
}

/**
 * ★ 合計金額のスマート表示（ホーム画面・カレンダー合計用）
 *
 * DECIMAL1対象の整数通貨（JPY/KRW/TWD/KZT/KGS）:
 *   - 端数があれば小数点第1位まで表示（例: 79.5円 → "¥79.5"）
 *   - 整数なら小数点なし（例: 530円 → "¥530"）
 * その他の通貨: 従来通り（formatCurrencyAmount と同じ）
 */
fun formatCurrencyAmountSmart(amount: Double): String {
    val code = getSelectedCurrencyCode()
    if (code in DECIMAL1_INPUT_CURRENCIES) {
        // ★ 小数部の有無を判定（浮動小数点誤差を考慮して0.001で判定）
        val hasDecimal = Math.abs(amount - Math.round(amount).toDouble()) > 0.001
        val digits = if (hasDecimal) 1 else 0
        return formatCurrencyAmountInternal(amount, digits)
    }
    return formatCurrencyAmountInternal(amount, getCurrencyFractionDigits())
}

/**
 * ★ 平均支出額の表示（カレンダー画面用）
 *
 * DECIMAL1対象の整数通貨（JPY/KRW/TWD/KZT/KGS）:
 *   - 常に小数点第2位まで表示（例: 456.78円 → "¥456.78"）
 *   - 割り算で端数が出やすいため固定桁で統一
 * その他の通貨: 従来通り（formatCurrencyAmount と同じ）
 */
fun formatCurrencyAmountForAverage(amount: Double): String {
    val code = getSelectedCurrencyCode()
    if (code in DECIMAL1_INPUT_CURRENCIES) {
        return formatCurrencyAmountInternal(amount, 2)
    }
    return formatCurrencyAmountInternal(amount, getCurrencyFractionDigits())
}

/**
 * 通貨フォーマット + "/day" サフィックス（合計用）
 */
fun formatCurrencyPerDay(amount: Double, perDaySuffix: String): String {
    return "${formatCurrencyAmountSmart(amount)} $perDaySuffix"
}

/**
 * ★ 通貨フォーマット + "/day" サフィックス（平均用・常時小数2桁）
 */
fun formatCurrencyPerDayForAverage(amount: Double, perDaySuffix: String): String {
    return "${formatCurrencyAmountForAverage(amount)} $perDaySuffix"
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

/**
 * ★ フォーマッター生成（fractionDigits を外部指定）
 */
private fun buildFormatter(locale: Locale, code: String, fractionDigits: Int): NumberFormat {
    val fmt = NumberFormat.getCurrencyInstance(locale)
    try { fmt.currency = Currency.getInstance(code) } catch (_: Exception) {}
    fmt.maximumFractionDigits = fractionDigits
    fmt.minimumFractionDigits = fractionDigits
    return fmt
}

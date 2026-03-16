package com.powder.simplebeertime.ui.settings

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),

    JAPANESE("ja"),

    SPANISH("es"),          // Español (España)
    SPANISH_MX("es-MX"),    // Español (México)
    ITALIAN("it"),
    PORTUGUESE_BR("pt-BR"),
    FRENCH("fr"),
    GERMAN("de"),
    ARABIC("ar"),

    INDONESIAN("in"),
    THAI("th"),
    TURKISH("tr"),
    VIETNAMESE("vi"),
    CHINESE_TRADITIONAL("zh-TW"),
    KOREAN("ko"),

    POLISH("pl"),
    ROMANIAN("ro"),
    UZBEK("uz"),
    KAZAKH("kk"),
    URDU("ur"),
    KYRGYZ("ky"),
    BULGARIAN("bg"),
    AZERBAIJANI("az"),

    // ── 英語地域バリアント（一覧下にまとめる）──
    ENGLISH("en"),       // English（汎用・その他地域）
    ENGLISH_US("en-US"),
    ENGLISH_GB("en-GB"),
    ENGLISH_CA("en-CA"),
    ENGLISH_AU("en-AU"),
    ENGLISH_IN("en-IN"),
    ENGLISH_PH("en-PH"),
    ENGLISH_SG("en-SG"),
    ENGLISH_ZA("en-ZA");
}

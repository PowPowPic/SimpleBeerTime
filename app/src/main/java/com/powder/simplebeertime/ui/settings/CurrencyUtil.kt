package com.powder.simplebeertime.ui.settings

fun currencySymbolFor(lang: AppLanguage): String {
    return when (lang) {
        AppLanguage.SYSTEM -> "$"
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
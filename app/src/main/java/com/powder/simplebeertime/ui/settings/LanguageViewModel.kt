package com.powder.simplebeertime.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powder.simplebeertime.data.preferences.LanguagePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LanguageViewModel(
    private val repository: LanguagePreferencesRepository
) : ViewModel() {

    private val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage

    init {
        viewModelScope.launch {
            repository.languageFlow.collectLatest { tag ->
                val lang = AppLanguage.entries.firstOrNull { it.tag == tag }
                    ?: AppLanguage.SYSTEM
                _appLanguage.value = lang
            }
        }
    }

    /**
     * 言語を設定し AppCompatDelegate で即時適用
     *
     * ★ AppCompatActivity を使っているため setApplicationLocales() が
     *    内部フックで Activity 再生成を自動トリガーする。
     *    restartApp() や recreate() の手動呼び出しは不要。
     */
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language.tag)
            if (language == AppLanguage.SYSTEM) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(language.tag)
                )
            }
        }
    }
}

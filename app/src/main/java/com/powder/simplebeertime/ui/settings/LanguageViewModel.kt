package com.powder.simplebeertime.ui.settings

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

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language.tag)
        }
    }

    suspend fun setLanguageSuspend(language: AppLanguage) {
        repository.setLanguage(language.tag)
    }
}
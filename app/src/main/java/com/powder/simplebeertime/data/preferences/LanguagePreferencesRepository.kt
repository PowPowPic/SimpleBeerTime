package com.powder.simplebeertime.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class LanguagePreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private const val DEFAULT_LANGUAGE_TAG = "system"
    }

    val languageFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: DEFAULT_LANGUAGE_TAG
    }

    suspend fun setLanguage(tag: String) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = tag
        }
    }

    fun getLanguageTagSync(): String = runBlocking {
        languageFlow.first()
    }
}
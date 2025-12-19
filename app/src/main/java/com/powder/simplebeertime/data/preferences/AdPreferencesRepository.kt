package com.powder.simplebeertime.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AdPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val LAST_AD_TIME_SLOT = stringPreferencesKey("last_ad_time_slot")

        // 現行踏襲（Stringで保存）
        val IS_AD_FREE = stringPreferencesKey("is_ad_free")
    }

    val lastAdTimeSlot: Flow<AdTimeSlot> = dataStore.data
        .map { preferences ->
            val slotName = preferences[PreferencesKeys.LAST_AD_TIME_SLOT] ?: AdTimeSlot.NONE.name

            // 旧enum(MORNING/EVENING)が残っていてもクラッシュしないよう保護
            runCatching { AdTimeSlot.valueOf(slotName) }
                .getOrElse { AdTimeSlot.NONE }
        }

    val isAdFree: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_AD_FREE]?.toBoolean() ?: false
        }

    suspend fun saveLastAdTimeSlot(timeSlot: AdTimeSlot) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_AD_TIME_SLOT] = timeSlot.name
        }
    }

    suspend fun setAdFree(isAdFree: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AD_FREE] = isAdFree.toString()
        }
    }
}

package com.powder.simplebeertime.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 既存の広告削除購入者の権利フラグを書き込むためのリポジトリ。
 *
 * 広告表示・広告頻度制御は撤去済みだが、過去の購入履歴を復元した際に
 * BillingManagerから権利状態を保存できるよう、この書き込み側だけを維持する。
 */
class AdPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        // 既存データとの互換性のため、保存キーとString形式は変更しない。
        val IS_AD_FREE = stringPreferencesKey("is_ad_free")
    }

    suspend fun setAdFree(isAdFree: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AD_FREE] = isAdFree.toString()
        }
    }
}

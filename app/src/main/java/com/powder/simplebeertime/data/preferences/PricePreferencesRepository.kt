package com.powder.simplebeertime.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PricePreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val KEY_PRICE_PER_BEER = floatPreferencesKey("price_per_beer")
        const val DEFAULT_PRICE = 5.00f   // デフォルトは $5.00（ビール1杯）
    }

    val pricePerBeer: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_PRICE_PER_BEER] ?: DEFAULT_PRICE
    }

    suspend fun updatePrice(newPrice: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_PRICE_PER_BEER] = newPrice
        }
    }
}
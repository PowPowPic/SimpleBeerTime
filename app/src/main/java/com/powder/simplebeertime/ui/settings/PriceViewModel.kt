package com.powder.simplebeertime.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powder.simplebeertime.data.preferences.PricePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PriceViewModel(
    private val repo: PricePreferencesRepository
) : ViewModel() {

    val pricePerBeer: Flow<Float> = repo.pricePerBeer

    fun updatePrice(newPrice: Float) {
        viewModelScope.launch {
            repo.updatePrice(newPrice)
        }
    }
}
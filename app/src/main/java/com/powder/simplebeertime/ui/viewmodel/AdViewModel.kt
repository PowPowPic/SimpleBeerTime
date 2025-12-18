package com.powder.simplebeertime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.powder.simplebeertime.data.preferences.AdPreferencesRepository
import com.powder.simplebeertime.data.preferences.AdTimeSlot
import com.powder.simplebeertime.util.AdTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdViewModel(private val repository: AdPreferencesRepository) : ViewModel() {

    suspend fun shouldShowAd(): Boolean {
        val isAdFree = repository.isAdFree.first()
        if (isAdFree) return false

        val currentSlot = AdTimeUtils.getCurrentAdTimeSlot()
        val lastSlot = repository.lastAdTimeSlot.first()

        return currentSlot != lastSlot
    }

    fun onAdShown() {
        viewModelScope.launch {
            repository.saveLastAdTimeSlot(AdTimeUtils.getCurrentAdTimeSlot())
        }
    }
}

class AdViewModelFactory(private val repository: AdPreferencesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

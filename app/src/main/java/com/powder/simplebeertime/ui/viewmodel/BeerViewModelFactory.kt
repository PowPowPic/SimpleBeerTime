package com.powder.simplebeertime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.powder.simplebeertime.data.repository.BeerRepository

class BeerViewModelFactory(
    private val repository: BeerRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BeerViewModel::class.java)) {
            return BeerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
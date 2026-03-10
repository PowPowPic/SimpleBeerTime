package com.powder.simplebeertime.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.powder.simplebeertime.ui.navigation.AppNavHost
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.PriceViewModel
import com.powder.simplebeertime.ui.viewmodel.AdViewModel
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import com.powder.simplebeertime.ui.viewmodel.RemoveAdsViewModel

@Composable
fun SimpleBeerTimeApp(
    beerViewModel: BeerViewModel,
    languageViewModel: LanguageViewModel,
    priceViewModel: PriceViewModel,
    adViewModel: AdViewModel,
    removeAdsViewModel: RemoveAdsViewModel
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(
                beerViewModel = beerViewModel,
                languageViewModel = languageViewModel,
                priceViewModel = priceViewModel,
                adViewModel = adViewModel,
                removeAdsViewModel = removeAdsViewModel
            )
        }
    }
}

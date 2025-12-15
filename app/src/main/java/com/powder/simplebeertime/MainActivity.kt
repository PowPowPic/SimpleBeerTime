package com.powder.simplebeertime

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.powder.simplebeertime.data.preferences.LanguagePreferencesRepository
import com.powder.simplebeertime.data.preferences.PricePreferencesRepository
import com.powder.simplebeertime.data.preferences.settingsDataStore
import com.powder.simplebeertime.ui.SimpleBeerTimeApp
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.LanguageViewModelFactory
import com.powder.simplebeertime.ui.settings.PriceViewModel
import com.powder.simplebeertime.ui.settings.PriceViewModelFactory
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import com.powder.simplebeertime.ui.viewmodel.BeerViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val beerViewModel: BeerViewModel by viewModels {
        val app = application as BeerApplication
        BeerViewModelFactory(
            app.container.beerRepository
        )
    }

    private val languageViewModel: LanguageViewModel by viewModels {
        LanguageViewModelFactory(
            LanguagePreferencesRepository(applicationContext.settingsDataStore)
        )
    }

    private val priceViewModel: PriceViewModel by viewModels {
        PriceViewModelFactory(
            PricePreferencesRepository(applicationContext.settingsDataStore)
        )
    }

    private fun tagToLocale(tag: String): Locale? {
        return when (tag) {
            "system" -> null
            "en" -> Locale("en")
            "ja" -> Locale("ja")
            "es" -> Locale("es")
            "it" -> Locale("it")
            "pt-BR" -> Locale("pt", "BR")
            "fr" -> Locale("fr")
            "de" -> Locale("de")
            "ar" -> Locale("ar")
            "in" -> Locale("in")
            "th" -> Locale("th")
            "tr" -> Locale("tr")
            "vi" -> Locale("vi")
            "zh-TW" -> Locale("zh", "TW")
            "ko" -> Locale("ko")
            else -> null
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val tag = runBlocking {
            val repo = LanguagePreferencesRepository(newBase.settingsDataStore)
            repo.languageFlow.first()
        }

        val locale = tagToLocale(tag)

        if (locale == null) {
            super.attachBaseContext(newBase)
            return
        }

        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SimpleBeerTimeApp(
                beerViewModel = beerViewModel,
                languageViewModel = languageViewModel,
                priceViewModel = priceViewModel
            )
        }
    }
}
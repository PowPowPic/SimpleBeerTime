package com.powder.simplebeertime

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
import com.powder.simplebeertime.util.ReviewHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * ★ ComponentActivity → AppCompatActivity に変更
 *
 * 【理由】
 * ComponentActivity + attachBaseContext(createConfigurationContext()) 方式では
 * 一部デバイス（Xiaomi 等）で stringResource() のリソース解決パスが
 * 差し替わらず、本文が英語のまま表示される問題があった。
 *
 * AppCompatActivity なら setApplicationLocales() が内部で
 * Configuration の差し替え＋Activity 再生成を一括で処理するため、
 * 全デバイスで確実に言語が反映される。
 * SBM・SSM・SSmT と同じ方式に統一。
 */
class MainActivity : AppCompatActivity() {

    private val beerViewModel: BeerViewModel by viewModels {
        val app = application as BeerApplication
        BeerViewModelFactory(app.container.beerRepository)
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

    /**
     * 保存済み言語設定を AppCompatDelegate で適用（SBM・SSM と同じパターン）
     */
    private fun applyStoredLanguage() {
        val languagePrefsRepo = LanguagePreferencesRepository(settingsDataStore)
        val tag = runBlocking {
            languagePrefsRepo.languageFlow.first()
        }
        if (tag == "system" || tag.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 保存された言語設定を適用（UIより前に実行）
        applyStoredLanguage()

        enableEdgeToEdge()

        // UMP同意画面を撤去したため、レビュー確認は起動時に直接実行する。
        ReviewHelper.checkAndRequest(this)

        setContent {
            SimpleBeerTimeApp(
                beerViewModel = beerViewModel,
                languageViewModel = languageViewModel,
                priceViewModel = priceViewModel
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as BeerApplication
        // 既存の広告削除購入者の権利を引き続きGoogle Playから確認する。
        app.container.billingManager.refreshPurchases()
    }
}

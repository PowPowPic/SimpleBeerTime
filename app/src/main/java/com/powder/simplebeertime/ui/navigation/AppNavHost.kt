package com.powder.simplebeertime.ui.navigation

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.powder.simplebeertime.ui.ads.TopBannerAd
import com.powder.simplebeertime.ui.screen.CalendarScreen
import com.powder.simplebeertime.ui.screen.GraphScreen
import com.powder.simplebeertime.ui.screen.HistoryScreen
import com.powder.simplebeertime.ui.screen.MainScreen
import com.powder.simplebeertime.ui.settings.LanguageSettingDialog
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.PriceSettingDialog
import com.powder.simplebeertime.ui.settings.PriceViewModel
import com.powder.simplebeertime.ui.settings.SettingsDialog
import com.powder.simplebeertime.ui.viewmodel.AdViewModel
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import com.powder.simplebeertime.util.AdManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.statusBarsPadding


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppNavHost(
    beerViewModel: BeerViewModel,
    languageViewModel: LanguageViewModel,
    priceViewModel: PriceViewModel,
    adViewModel: AdViewModel,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- ダイアログの状態（一元管理） ---
    val showSettingsDialog = remember { mutableStateOf(false) }
    val showLanguageDialog = remember { mutableStateOf(false) }
    val showPriceDialog = remember { mutableStateOf(false) }

    // --- 単価の状態 ---
    val priceState = priceViewModel
        .pricePerBeer
        .collectAsState(initial = 5.00f)

    // --- 広告削除状態（上部バナー表示用） ---
    val isAdFreeState = adViewModel.isAdFree.collectAsState(initial = false)

    // --- 画面定義 ---
    val screens = listOf(
        Screen.Main,
        Screen.History,
        Screen.Calendar,
        Screen.Graph
    )

    // --- Pager状態 ---
    val pagerState = rememberPagerState(pageCount = { screens.size })

    // --- 広告表示とGraph遷移（Graphタップ時のみ） ---
    fun navigateToGraph() {
        scope.launch {
            if (adViewModel.shouldShowAd()) {
                AdManager.showInterstitial(
                    activity = context as Activity,
                    onAdClosed = {
                        scope.launch {
                            pagerState.animateScrollToPage(screens.indexOf(Screen.Graph))
                        }
                    },
                    onAdShown = {
                        // ★広告が実際に表示され、閉じられたときだけ枠を消費
                        adViewModel.onAdShown()
                    }
                )
            } else {
                pagerState.animateScrollToPage(screens.indexOf(Screen.Graph))
            }
        }
    }

    // --- 連動ロジックA（Pager→Nav） ---
    LaunchedEffect(pagerState.currentPage) {
        val targetScreen = screens[pagerState.currentPage]
        val currentRoute = navController.currentDestination?.route

        if (currentRoute != targetScreen.route) {
            navController.navigate(targetScreen.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // --- 連動ロジックB（Nav→Pager） ---
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            val pageIndex = screens.indexOfFirst { it.route == route }
            if (pageIndex >= 0 && pageIndex != pagerState.currentPage) {
                // スワイプでも広告を出さない方針なので、ここでは広告処理なし
                pagerState.animateScrollToPage(pageIndex)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,

        // ★上部バナー（全画面共通）
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()   // ★これを追加
                    .wrapContentHeight()
            ) {
                TopBannerAd(
                    isAdFree = isAdFreeState.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        },


        bottomBar = {
            NavBar(
                navController = navController,
                onSettingsClick = { showSettingsDialog.value = true },
                onGraphClick = { navigateToGraph() }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8E1),  // 薄い黄色
                            Color(0xFFFFB300)   // 琥珀色
                        )
                    )
                )
                .padding(innerPadding)
        ) {

            // --- ダミーNavHost（NavBar状態維持用） ---
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Main.route
                ) {
                    composable(Screen.Main.route) { }
                    composable(Screen.History.route) { }
                    composable(Screen.Calendar.route) { }
                    composable(Screen.Graph.route) { }
                }
            }

            // --- 画面本体（Pager） ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                when (screens[page]) {
                    Screen.Main -> {
                        MainScreen(
                            viewModel = beerViewModel,
                            languageViewModel = languageViewModel,
                            pricePerBeer = priceState.value,
                            onSettingsClick = { showSettingsDialog.value = true }
                        )
                    }

                    Screen.History -> {
                        HistoryScreen(viewModel = beerViewModel)
                    }

                    Screen.Calendar -> {
                        CalendarScreen(
                            viewModel = beerViewModel,
                            languageViewModel = languageViewModel,
                            pricePerBeer = priceState.value
                        )
                    }

                    Screen.Graph -> {
                        GraphScreen(viewModel = beerViewModel)
                    }

                    else -> {}
                }
            }

            // --- ダイアログ表示（一元管理） ---

            // 1) 設定メニュー
            if (showSettingsDialog.value) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog.value = false },
                    onLanguageSettingClick = {
                        showSettingsDialog.value = false
                        showLanguageDialog.value = true
                    },
                    onPriceSettingClick = {
                        showSettingsDialog.value = false
                        showPriceDialog.value = true
                    },
                    onConfirmDeleteAll = {
                        beerViewModel.deleteAllRecords()
                    }
                )
            }

            // 2) 言語設定
            if (showLanguageDialog.value) {
                LanguageSettingDialog(
                    languageViewModel = languageViewModel,
                    onDismiss = { showLanguageDialog.value = false }
                )
            }

            // 3) 単価設定
            if (showPriceDialog.value) {
                PriceSettingDialog(
                    currentPrice = priceState.value,
                    onConfirm = { newPrice ->
                        priceViewModel.updatePrice(newPrice)
                        showPriceDialog.value = false
                    },
                    onDismiss = { showPriceDialog.value = false }
                )
            }
        }
    }
}

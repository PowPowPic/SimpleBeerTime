package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.currencySymbolFor
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: BeerViewModel,
    languageViewModel: LanguageViewModel,
    pricePerBeer: Float,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latest by viewModel.latestRecord.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()

    // 言語設定から通貨記号を取得
    val currentLang by languageViewModel.appLanguage.collectAsState()
    val currencySymbol = currencySymbolFor(currentLang)

    // ▼ 経過時間タイマー
    val elapsedMillis by produceState(initialValue = 0L, key1 = latest?.timestamp) {
        if (latest == null) {
            value = 0L
            return@produceState
        }
        while (true) {
            value = System.currentTimeMillis() - (latest?.timestamp ?: System.currentTimeMillis())
            delay(1000)
        }
    }
    val elapsedText = formatElapsedTime(elapsedMillis)

    // ▼ 支出計算
    val weekCostTotal = weekStats.count * pricePerBeer
    val todayCost = todayStats.count * pricePerBeer

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // 🔶 広告スペース
        Spacer(modifier = Modifier.height(90.dp))

        // 🪪 カード1：今週の本数＆平均
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.Card)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        R.string.main_week_count,
                        weekStats.count
                    ),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.main_week_avg,
                        weekStats.avgPerDay
                    ),
                    color = SimpleColors.TextPrimary
                )
            }
        }

        // 🪪 カード2：今週の支出
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.Card)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.label_week_cost),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SimpleColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.main_week_cost,
                        currencySymbol,
                        weekCostTotal
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = SimpleColors.TextPrimary
                )
            }
        }

        // 🪪 カード3：今日の本数＆支出
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.Card)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        R.string.main_today_count,
                        todayStats.count
                    ),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.main_today_cost,
                        currencySymbol,
                        todayCost
                    ),
                    color = SimpleColors.TextPrimary
                )
            }
        }

        // ⏱ 直近の飲酒から
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.label_time_since_last),
                style = MaterialTheme.typography.bodySmall,
                color = SimpleColors.TextSecondary
            )
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.headlineMedium,
                color = SimpleColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🍺 ボタン: Log 1 beer
        Button(
            onClick = { viewModel.insertBeer() },
            colors = ButtonDefaults.buttonColors(containerColor = SimpleColors.ButtonPrimary)
        ) {
            Text(
                text = stringResource(R.string.main_button_add_one),
                color = SimpleColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🍺 ボタン: Undo last beer
        Button(
            onClick = { viewModel.deleteLatestBeer() },
            colors = ButtonDefaults.buttonColors(containerColor = SimpleColors.ButtonPrimary)
        ) {
            Text(
                text = stringResource(R.string.main_button_undo_last),
                color = SimpleColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ① 3時ルール
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.main_midnight_rule_note),
                modifier = Modifier.fillMaxWidth(0.9f),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodySmall,
                color = SimpleColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ② 青文字リンク
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You can change the language\nand price per beer in Settings.",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodySmall,
                color = SimpleColors.PureBlue
            )
        }
    }
}

private fun formatElapsedTime(millis: Long): String {
    if (millis <= 0L) return "--:--:--"

    val totalSeconds = millis / 1000
    val seconds = (totalSeconds % 60).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    val hours = (totalSeconds / 3600).toInt()

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
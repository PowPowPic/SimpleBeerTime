package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.currencySymbolFor
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: BeerViewModel,
    languageViewModel: LanguageViewModel,
    pricePerBeer: Float,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayStats by viewModel.todayStats.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()

    // 言語設定から通貨記号を取得
    val currentLang by languageViewModel.appLanguage.collectAsState()
    val currencySymbol = currencySymbolFor(currentLang)

    // 小数入力用の状態
    var amountText by remember { mutableStateOf("") }

    // 小数入力を処理する関数
    fun addCustomAmount() {
        val raw = amountText.toDoubleOrNull() ?: return
        if (raw <= 0) return
        // 小数第1位まで丸める
        val v = (raw * 10).roundToInt() / 10.0
        viewModel.insertBeer(amount = v)
        amountText = ""
    }

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

        Spacer(modifier = Modifier.height(16.dp))

        // 🍺 小数入力エリア: 約 [ 1.4 ] beers [Add]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.main_amount_prefix),
                color = SimpleColors.TextPrimary
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { new ->
                    // 数字と小数点のみ許可
                    if (new.matches(Regex("""^\d*\.?\d*$"""))) {
                        amountText = new
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "1.4",
                        color = SimpleColors.TextSecondary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { addCustomAmount() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SimpleColors.TextPrimary,
                    unfocusedTextColor = SimpleColors.TextPrimary,
                    cursorColor = SimpleColors.TextPrimary,
                    focusedBorderColor = SimpleColors.ButtonPrimary,
                    unfocusedBorderColor = SimpleColors.TextSecondary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.main_amount_suffix),
                color = SimpleColors.TextPrimary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { addCustomAmount() },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SimpleColors.ButtonPrimary)
            ) {
                Text(
                    text = stringResource(R.string.main_button_add),
                    color = SimpleColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
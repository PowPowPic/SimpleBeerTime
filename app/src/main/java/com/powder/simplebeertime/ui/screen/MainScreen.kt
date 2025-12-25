package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    // ✅ placeholder ではなく「実値」として初期値を入れる（そのままAdd/Doneで反映される）
    var amountText by rememberSaveable { mutableStateOf("1.4") }

    // 小数入力を処理する関数
    fun addCustomAmount() {
        // ✅ 空欄のままOK/Doneを押した場合も「1.4」として扱いたい
        val rawText = amountText.trim().ifEmpty { "1.4" }

        val raw = rawText.toDoubleOrNull() ?: return
        if (raw <= 0) {
            // 0以下は何もしないが、入力欄はデフォルトに戻す
            amountText = "1.4"
            return
        }

        // 小数第1位まで丸める
        val v = (raw * 10).roundToInt() / 10.0
        viewModel.insertBeer(amount = v)

        // ✅ 追加後は毎回 1.4 に戻す（ユーザーが毎回打ち直さなくてよい）
        amountText = "1.4"
    }

    // ▼ 支出計算
    val weekCostTotal = weekStats.count * pricePerBeer
    val todayCost = todayStats.count * pricePerBeer

    // カードグラデーション
    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            SimpleColors.CardStart,
            SimpleColors.CardEnd,
            SimpleColors.CardStart
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            // ✅ 履歴画面と同じ横paddingに揃える（位置が揃った感が出る）
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // ✅ 履歴画面と同じ「広告スペース（詰める）」に合わせる
        Spacer(modifier = Modifier.height(12.dp))

        // 🪪 カード1：今週の本数＆平均
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardGradient)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.main_week_count, weekStats.count),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.main_week_avg, weekStats.avgPerDay),
                    color = SimpleColors.TextPrimary
                )
            }
        }

        // 🪪 カード2：今週の支出
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardGradient)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.label_week_cost),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SimpleColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.main_week_cost, currencySymbol, weekCostTotal),
                    style = MaterialTheme.typography.titleMedium,
                    color = SimpleColors.TextPrimary
                )
            }
        }

        // 🪪 カード3：今日の本数＆支出
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardGradient)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.main_today_count, todayStats.count),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.main_today_cost, currencySymbol, todayCost),
                    color = SimpleColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🍺 ボタン: Log 1 beer（グラデーション）
        GradientButton(
            text = stringResource(R.string.main_button_add_one),
            onClick = { viewModel.insertBeer() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🍺 小数入力エリア: About [ 1.4 ] beers [Add]
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

            // Addボタン（グラデーション）
            GradientButton(
                text = stringResource(R.string.main_button_add),
                onClick = { addCustomAmount() },
                width = 70.dp,
                height = 40.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🍺 ボタン: Undo last beer（グラデーション）
        GradientButton(
            text = stringResource(R.string.main_button_undo_last),
            onClick = { viewModel.deleteLatestBeer() }
        )

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

        // ✅ 残りスペースを押し下げて、青文字リンクを「ナビバー直上」へ
        Spacer(modifier = Modifier.weight(1f))

        // ② 青文字リンク（ベタ書き英語はシリーズ共通仕様）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // ✅ 直上に寄せたいので bottom padding は最小（Scaffold側の bottom padding を信頼）
                .padding(bottom = 4.dp)
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

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp = 180.dp,
    height: androidx.compose.ui.unit.Dp = 44.dp
) {
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(
            SimpleColors.ButtonStart,
            SimpleColors.ButtonEnd,
            SimpleColors.ButtonStart
        )
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(buttonGradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

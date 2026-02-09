package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.currencySymbolFor
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: BeerViewModel,
    languageViewModel: LanguageViewModel,
    pricePerBeer: Float,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val todayStats by viewModel.todayStats.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()

    // 言語設定から通貨記号を取得
    val currentLang by languageViewModel.appLanguage.collectAsState()
    val currencySymbol = currencySymbolFor(currentLang)

    // 日付をライフサイクルに連動して更新
    var displayDate by remember { mutableStateOf(java.time.LocalDate.now()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                displayDate = java.time.LocalDate.now()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ロケールに応じた日付フォーマット
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.getDefault())
    }

    // 小数入力用の状態
    var amountText by rememberSaveable { mutableStateOf("1.4") }

    // 小数入力を処理する関数
    fun addCustomAmount() {
        val rawText = amountText.trim().ifEmpty { "1.4" }

        val raw = rawText.toDoubleOrNull() ?: return
        if (raw <= 0) {
            amountText = "1.4"
            return
        }

        // 小数第1位まで丸める
        val v = (raw * 10).roundToInt() / 10.0
        viewModel.insertBeer(amount = v)

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

    // ★ ルール①：スクロール可能にする
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 今日の日付（ロケール対応）
        Text(
            text = displayDate.format(dateFormatter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SimpleColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ★ ルール③：カードは可変（heightIn使用、height()は使わない）
        // 🪪 カード1：今週の本数＆平均
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardGradient)
                .padding(vertical = 10.dp)
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
                .padding(vertical = 10.dp)
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
                .padding(vertical = 10.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

        // ★ ルール②：ボタンは可変＋最低タップサイズ保証
        // 🍺 ボタン: Log 1 beer（グラデーション）
        GradientButton(
            text = stringResource(R.string.main_button_add_one),
            onClick = { viewModel.insertBeer() }
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                minWidth = 70.dp,
                minHeight = 48.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🍺 ボタン: Undo last beer（グラデーション）
        GradientButton(
            text = stringResource(R.string.main_button_undo_last),
            onClick = { viewModel.deleteLatestBeer() }
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(6.dp))

        // ② 通貨記号の案内文（赤字）
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.main_currency_hint),
                modifier = Modifier.fillMaxWidth(0.9f),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF0000)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ③ 青文字リンク（ベタ書き英語はシリーズ共通仕様）
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

/**
 * ★ ルール②：ボタンサイズは可変（最低タップサイズ48dp保証）
 */
@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    minWidth: Dp = 180.dp,
    minHeight: Dp = 48.dp
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
            .widthIn(min = minWidth)
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(22.dp))
            .background(buttonGradient)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

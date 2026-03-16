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
import com.powder.simplebeertime.ui.settings.formatBeerCount
import com.powder.simplebeertime.ui.settings.formatCurrencyAmount
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: BeerViewModel,
    pricePerBeer: Float,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val todayStats by viewModel.todayStats.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()

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

    // ★ アプリの有効ロケールを AppCompatDelegate から取得
    //   AppCompatDelegate.getApplicationLocales() は setApplicationLocales() で
    //   設定された正確なロケールを返す。空の場合はシステム言語にフォールバック。
    //   remember で1回だけ計算（Activity 再生成で再計算される）
    val appLocale = remember {
        val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val locale = if (appLocales.isEmpty) {
            Locale.getDefault()
        } else {
            appLocales[0] ?: Locale.getDefault()
        }
        // en-ZA は ICU 規格が "," だが実生活慣習に合わせて "." を強制
        if (locale.country == "ZA") Locale.US else locale
    }

    // ロケールに応じた日付フォーマット
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(appLocale)
    }

    // ★ ロケール対応: デフォルト値をロケールの小数点記号で表示
    //   例: fr → "1,4" / en → "1.4" / de → "1,4"
    val defaultAmountText = remember {
        String.format(appLocale, "%.1f", 1.4)
    }

    // 小数入力用の状態
    // ★ ロケール変更時にデフォルト値をリセットするため、appLocale を key に指定
    //   rememberSaveable は Activity 再生成で値を復元するが、
    //   ロケールが変わった場合は旧ロケールの小数点記号（例: "1,4"）が残ってしまう。
    //   key にロケールを入れることで、言語変更時にリセットされる。
    var amountText by rememberSaveable(appLocale.toString()) { mutableStateOf(defaultAmountText) }

    // 小数入力を処理する関数
    fun addCustomAmount() {
        val rawText = amountText.trim().ifEmpty { defaultAmountText }

        // ★ ロケール対応: NumberFormat.parse()でカンマ小数点も正しくパース
        val raw = try {
            java.text.NumberFormat.getInstance(appLocale).parse(rawText)?.toDouble()
        } catch (e: Exception) {
            rawText.replace(',', '.').toDoubleOrNull()
        } ?: return
        if (raw <= 0) {
            amountText = defaultAmountText
            return
        }

        // 小数第1位まで丸める
        val v = (raw * 10).roundToInt() / 10.0
        viewModel.insertBeer(amount = v)

        amountText = defaultAmountText
    }

    // ▼ 支出計算
    val weekCostTotal = weekStats.count * pricePerBeer
    val todayCost = todayStats.count * pricePerBeer

    // ★ スマート通貨フォーマット（CurrencyUtil使用）
    val weekCostText = formatCurrencyAmount(weekCostTotal.toDouble())
    val todayCostText = formatCurrencyAmount(todayCost.toDouble())

    // ★ スマート本数フォーマット（整数なら小数点なし）
    val weekCountText = formatBeerCount(weekStats.count)
    val weekAvgText = formatBeerCount(weekStats.avgPerDay)
    val todayCountText = formatBeerCount(todayStats.count)

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

        // 🪪 カード1：今週の本数＆平均
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardGradient)
                .padding(vertical = 7.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ★ スマートフォーマット適用
                Text(
                    text = stringResource(R.string.main_week_count, weekCountText),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.main_week_avg, weekAvgText),
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
                .padding(vertical = 7.dp)
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
                Spacer(modifier = Modifier.height(2.dp))
                // ★ スマート通貨フォーマット適用
                Text(
                    text = weekCostText,
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
                .padding(vertical = 7.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ★ スマートフォーマット適用
                Text(
                    text = stringResource(R.string.main_today_count, todayCountText),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                // ★ スマート通貨フォーマット適用
                Text(
                    text = stringResource(R.string.main_today_cost, todayCostText),
                    color = SimpleColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                    // ★ ロケール対応: ドットとカンマの両方を許可
                    if (new.matches(Regex("""^\d*[.,]?\d*$"""))) {
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

        // ③ 青文字リンク
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

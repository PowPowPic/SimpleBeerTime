package com.powder.simplebeertime.ui.screen

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powder.simplebeertime.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.powder.simplebeertime.ui.dialog.EditDayAmountDialog
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.currencySymbolFor
import com.powder.simplebeertime.ui.settings.formatBeerCount
import com.powder.simplebeertime.ui.settings.formatCurrencyAmount
import com.powder.simplebeertime.ui.settings.formatCurrencyPerDay
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import com.powder.simplebeertime.util.currentLogicalDate
import com.powder.simplebeertime.util.toLogicalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: BeerViewModel,
    languageViewModel: LanguageViewModel,
    pricePerBeer: Float,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.allRecords.collectAsState(initial = emptyList())

    val currentLang by languageViewModel.appLanguage.collectAsState()
    val currencySymbol = currencySymbolFor(currentLang)

    var dialogTargetDate by remember { mutableStateOf<LocalDate?>(null) }

    val logicalToday = remember { currentLogicalDate(cutoffHour = 3) }
    val currentMonthFirst = remember { logicalToday.withDayOfMonth(1) }

    var monthDate by rememberSaveable {
        mutableStateOf(logicalToday.withDayOfMonth(1))
    }

    // ★ グラフからのナビゲーション要求を監視
    val navigateRequest by viewModel.navigateToCalendarMonth.collectAsState()
    LaunchedEffect(navigateRequest) {
        navigateRequest?.let { targetMonth ->
            monthDate = targetMonth
            viewModel.onCalendarNavigationHandled()
        }
    }

    // ★ 今月を表示中かどうか
    val isCurrentMonth = monthDate == currentMonthFirst

    val recordsForMonth = remember(allRecords, monthDate) {
        val month = monthDate.month
        val year = monthDate.year
        allRecords
            .map { record ->
                val date = record.timestamp.toLogicalDate(cutoffHour = 3)
                date to record
            }
            .filter { (date, _) ->
                date.year == year && date.month == month
            }
    }

    val dailyCounts = remember(recordsForMonth) {
        val map = mutableMapOf<LocalDate, Double>()
        recordsForMonth.forEach { (date, record) ->
            map[date] = (map[date] ?: 0.0) + record.amount
        }
        map
    }

    val totalBeers: Double = dailyCounts.values.sum()

    val daysToUse: Int = if (monthDate.year == logicalToday.year && monthDate.month == logicalToday.month) {
        logicalToday.dayOfMonth
    } else {
        monthDate.lengthOfMonth()
    }

    val avgBeersPerDay: Double = if (daysToUse > 0) totalBeers / daysToUse.toDouble() else 0.0

    val price: Double = pricePerBeer.toDouble()
    val totalCost: Double = totalBeers * price
    val avgCostPerDay: Double = if (daysToUse > 0) totalCost / daysToUse.toDouble() else 0.0

    // ★ スマート通貨フォーマット
    val totalCostText = formatCurrencyAmount(currentLang, currencySymbol, totalCost)

    // ★ "/day" サフィックスをstringResourceから取得
    val perDaySuffix = stringResource(R.string.calendar_per_day_suffix)
    val avgCostPerDayText = formatCurrencyPerDay(currentLang, currencySymbol, avgCostPerDay, perDaySuffix)

    // ★ スマート本数フォーマット
    val totalBeersText = formatBeerCount(totalBeers)
    val avgBeersText = formatBeerCount(avgBeersPerDay)

    val dateLabelFormatter = remember {
        DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
    }

    val currentLocale = Locale.getDefault()
    val monthFormatter = remember(currentLocale) {
        val pattern = DateFormat.getBestDateTimePattern(currentLocale, "yyyyMMM")
        DateTimeFormatter.ofPattern(pattern, currentLocale)
    }
    val monthTitle = remember(monthDate, monthFormatter) {
        monthDate.format(monthFormatter)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { monthDate = monthDate.minusMonths(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.calendar_cd_previous_month),
                    tint = SimpleColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ★ 月タイトルタップで今月に戻る
            Text(
                text = monthTitle,
                fontSize = 20.sp,
                color = if (isCurrentMonth) SimpleColors.TextPrimary else SimpleColors.PureBlue,
                textDecoration = if (isCurrentMonth) TextDecoration.None else TextDecoration.Underline,
                modifier = if (isCurrentMonth) {
                    Modifier
                } else {
                    Modifier.clickable {
                        monthDate = currentMonthFirst
                    }
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            val canGoNext = monthDate.isBefore(logicalToday.withDayOfMonth(1))
            IconButton(
                onClick = { if (canGoNext) monthDate = monthDate.plusMonths(1) },
                enabled = canGoNext
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.calendar_cd_next_month),
                    tint = SimpleColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        WeekdayHeader()

        Spacer(modifier = Modifier.height(4.dp))

        MonthGrid(
            monthDate = monthDate,
            dailyCounts = dailyCounts,
            logicalToday = logicalToday,
            onDayClick = { date -> dialogTargetDate = date }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ★ タップヒント（赤文字）
        // ★ タップヒント（赤文字）
        Text(
            text = stringResource(R.string.calendar_tap_hint),
            color = Color.Red,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // ★ スマートフォーマット適用
        MonthlySummarySection(
            totalBeersText = totalBeersText,
            avgBeersText = avgBeersText,
            perDaySuffix = perDaySuffix,
            totalCostText = totalCostText,
            avgCostPerDayText = avgCostPerDayText
        )
        // ★ 日付タップ編集ダイアログ
        dialogTargetDate?.let { targetDate ->
            EditDayAmountDialog(
                dateTitle = targetDate.format(dateLabelFormatter),
                currentAmount = dailyCounts[targetDate] ?: 0.0,
                onConfirm = { newAmount ->
                    viewModel.updateDayAmount(targetDate, newAmount)
                    dialogTargetDate = null
                },
                onDismiss = { dialogTargetDate = null }
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val labels = listOf(
            stringResource(R.string.weekday_mon),
            stringResource(R.string.weekday_tue),
            stringResource(R.string.weekday_wed),
            stringResource(R.string.weekday_thu),
            stringResource(R.string.weekday_fri),
            stringResource(R.string.weekday_sat),
            stringResource(R.string.weekday_sun)
        )
        labels.forEach { label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = SimpleColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    monthDate: LocalDate,
    dailyCounts: Map<LocalDate, Double>,
    logicalToday: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = monthDate
    val daysInMonth = firstDayOfMonth.lengthOfMonth()
    val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value - 1

    val cells = mutableListOf<Int?>()
    repeat(firstDayOfWeekIndex) { cells.add(null) }
    for (day in 1..daysInMonth) cells.add(day)
    while (cells.size % 7 != 0) cells.add(null)

    val rows: List<List<Int?>> = cells.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        rows.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val date = monthDate.withDayOfMonth(day)
                            val isFutureDay = date.isAfter(logicalToday)
                            DayCell(
                                day = day,
                                count = dailyCounts[date] ?: 0.0,
                                isFutureDay = isFutureDay,
                                onClick = { if (!isFutureDay) onDayClick(date) }
                            )
                        } else {
                            DayCellEmpty()
                        }
                    }
                }
            }
        }
    }
}

/**
 * ★ 飲酒なしの日は青文字で「0」を表示
 *    未来日は数値なし
 *    本数もスマートフォーマット（整数なら小数点なし）
 */
@Composable
private fun DayCell(
    day: Int,
    count: Double,
    isFutureDay: Boolean,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (isFutureDay) Modifier else Modifier.clickable { onClick() }
    ) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            color = SimpleColors.TextPrimary
        )

        if (isFutureDay) {
            Spacer(modifier = Modifier.height(14.dp))
        } else if (count > 0.0) {
            Spacer(modifier = Modifier.height(2.dp))
            // ★ スマートフォーマット（整数なら小数点なし）
            Text(
                text = formatBeerCount(count),
                fontSize = 12.sp,
                color = SimpleColors.PureRed
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "0",
                fontSize = 12.sp,
                color = SimpleColors.PureBlue
            )
        }
    }
}

@Composable
private fun DayCellEmpty() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = " ", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(14.dp))
    }
}

/**
 * ★ サマリーセクション（スマートフォーマット版）
 */
@Composable
fun MonthlySummarySection(
    totalBeersText: String,
    avgBeersText: String,
    perDaySuffix: String,
    totalCostText: String,
    avgCostPerDayText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        HorizontalDivider(color = SimpleColors.TextSecondary.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LabelValueBlock(
                label = stringResource(R.string.calendar_total_beers_label),
                value = totalBeersText,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            LabelValueBlock(
                label = stringResource(R.string.calendar_daily_average_label),
                value = "$avgBeersText $perDaySuffix",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LabelValueBlock(
                label = stringResource(R.string.calendar_total_cost_label),
                value = totalCostText,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            LabelValueBlock(
                label = stringResource(R.string.calendar_average_cost_label),
                value = avgCostPerDayText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LabelValueBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
            color = SimpleColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Start,
            color = SimpleColors.TextPrimary
        )
    }
}

package com.powder.simplebeertime.ui.screen

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.settings.LanguageViewModel
import com.powder.simplebeertime.ui.settings.currencySymbolFor
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

    val logicalToday = remember { currentLogicalDate(cutoffHour = 3) }

    var monthDate by rememberSaveable {
        mutableStateOf(logicalToday.withDayOfMonth(1))
    }

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

    // ✅ LocalDate -> Double（日別合計）
    val dailyCounts = remember(recordsForMonth) {
        val map = mutableMapOf<LocalDate, Double>()
        recordsForMonth.forEach { (date, record) ->
            map[date] = (map[date] ?: 0.0) + record.amount
        }
        map
    }

    // ✅ 量は全部 Double で統一
    val totalBeers: Double = dailyCounts.values.sum()
    val daysInMonth: Int = monthDate.lengthOfMonth()
    val avgBeersPerDay: Double = if (daysInMonth > 0) totalBeers / daysInMonth.toDouble() else 0.0

    // ✅ 金額も Double で統一（pricePerBeer は Float なので Double化）
    val price: Double = pricePerBeer.toDouble()
    val totalCost: Double = totalBeers * price
    val avgCostPerDay: Double = if (daysInMonth > 0) totalCost / daysInMonth.toDouble() else 0.0

    // ✅ 通貨文字列（strings 側の %1$s%2$.2f 前提）
    val totalCostText = String.format(
        Locale.getDefault(),
        stringResource(R.string.format_currency_amount),
        currencySymbol,
        totalCost
    )
    val avgCostPerDayText = String.format(
        Locale.getDefault(),
        stringResource(R.string.format_currency_per_day),
        currencySymbol,
        avgCostPerDay
    )

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

            Text(
                text = monthTitle,
                fontSize = 20.sp,
                color = SimpleColors.TextPrimary
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
            dailyCounts = dailyCounts
        )

        Spacer(modifier = Modifier.height(16.dp))

        MonthlySummarySection(
            totalBeers = totalBeers,
            avgBeersPerDay = avgBeersPerDay,
            totalCostText = totalCostText,
            avgCostPerDayText = avgCostPerDayText
        )
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
    dailyCounts: Map<LocalDate, Double>
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
                            DayCell(
                                day = day,
                                count = dailyCounts[date] ?: 0.0
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

@Composable
private fun DayCell(
    day: Int,
    count: Double
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            color = SimpleColors.TextPrimary
        )

        if (count > 0.0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.1f", count),
                fontSize = 12.sp,
                color = SimpleColors.PureRed
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
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

@Composable
fun MonthlySummarySection(
    totalBeers: Double,
    avgBeersPerDay: Double,
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
                value = String.format(Locale.getDefault(), "%.1f", totalBeers),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            LabelValueBlock(
                label = stringResource(R.string.calendar_daily_average_label),
                value = String.format(
                    Locale.getDefault(),
                    stringResource(R.string.format_daily_average_beers),
                    avgBeersPerDay
                ),
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

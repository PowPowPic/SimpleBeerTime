package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.dialog.DeleteDayConfirmDialog
import com.powder.simplebeertime.ui.dialog.EditDayAmountDialog
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import com.powder.simplebeertime.util.currentLogicalDate
import com.powder.simplebeertime.util.toLogicalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: BeerViewModel,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.allRecords.collectAsState(initial = emptyList())
    val logicalToday = remember { currentLogicalDate(cutoffHour = 3) }

    // 現在表示している週の月曜日
    var weekMonday by rememberSaveable {
        mutableStateOf(logicalToday.with(DayOfWeek.MONDAY))
    }

    // 週の日曜日
    val weekSunday = weekMonday.plusDays(6)

    // 今週の月曜日（未来の週には進めないように）
    val currentWeekMonday = logicalToday.with(DayOfWeek.MONDAY)

    // ダイアログ用の状態
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }
    var deletingDate by remember { mutableStateOf<LocalDate?>(null) }

    // 週の各曜日の合計を計算
    val weekValues: List<Double> = remember(allRecords, weekMonday) {
        (0..6).map { dayOffset ->
            val date = weekMonday.plusDays(dayOffset.toLong())
            allRecords
                .filter { record ->
                    record.timestamp.toLogicalDate(cutoffHour = 3) == date
                }
                .sumOf { it.amount }
        }
    }

    // 週合計
    val weekTotal = weekValues.sum()

    // 週平均
    val weekAverage = weekTotal / 7.0

    // 色分け用の判定
    val max = weekValues.maxOrNull() ?: 0.0
    val min = weekValues.minOrNull() ?: 0.0
    val allZero = weekValues.all { it == 0.0 }
    val allSame = weekValues.all { it == weekValues.first() }

    fun valueColor(value: Double): Color = when {
        allZero -> SimpleColors.PureBlue
        allSame && weekValues.first() != 0.0 -> SimpleColors.TextPrimary
        value == max -> SimpleColors.PureRed
        value == min -> SimpleColors.PureBlue
        else -> SimpleColors.TextPrimary
    }

    // 曜日ラベル
    val dayLabels = listOf(
        stringResource(R.string.weekday_mon),
        stringResource(R.string.weekday_tue),
        stringResource(R.string.weekday_wed),
        stringResource(R.string.weekday_thu),
        stringResource(R.string.weekday_fri),
        stringResource(R.string.weekday_sat),
        stringResource(R.string.weekday_sun)
    )

    // 日付フォーマット（ロケール対応）
    val currentLocale = Locale.getDefault()
    val dateFormatter = remember(currentLocale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(
            currentLocale,
            "MMMd"
        )
        DateTimeFormatter.ofPattern(pattern, currentLocale)
    }

    val weekRangeText = remember(weekMonday, weekSunday, dateFormatter) {
        "${weekMonday.format(dateFormatter)} - ${weekSunday.format(dateFormatter)}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // 広告スペース
        Spacer(modifier = Modifier.height(30.dp))

        // 週ナビゲーション
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { weekMonday = weekMonday.minusWeeks(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.history_cd_previous_week),
                    tint = SimpleColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = weekRangeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SimpleColors.TextPrimary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 未来の週には進めない
            val canGoNext = weekMonday.isBefore(currentWeekMonday)
            IconButton(
                onClick = { if (canGoNext) weekMonday = weekMonday.plusWeeks(1) },
                enabled = canGoNext
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.history_cd_next_week),
                    tint = if (canGoNext) SimpleColors.TextPrimary else SimpleColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // サマリーセクション（センタリング）
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.history_week_summary_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SimpleColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.history_week_total, weekTotal),
                fontSize = 16.sp,
                color = SimpleColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.history_week_avg, weekAverage),
                fontSize = 16.sp,
                color = SimpleColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 曜日カード（Mon〜Sun）
        dayLabels.forEachIndexed { index, label ->
            val value = weekValues[index]
            val date = weekMonday.plusDays(index.toLong())
            val dateText = date.format(dateFormatter)

            DayCard(
                dayLabel = label,
                dateText = dateText,
                value = value,
                valueColor = valueColor(value),
                onEditClick = { editingDate = date },
                onDeleteClick = { deletingDate = date }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // 編集ダイアログ
    editingDate?.let { date ->
        val currentAmount = weekValues.getOrNull(
            (date.toEpochDay() - weekMonday.toEpochDay()).toInt()
        ) ?: 0.0

        EditDayAmountDialog(
            currentAmount = currentAmount,
            onDismiss = { editingDate = null },
            onConfirm = { newAmount ->
                viewModel.updateDayAmount(date, newAmount)
                editingDate = null
            }
        )
    }

    // 削除確認ダイアログ
    deletingDate?.let { date ->
        DeleteDayConfirmDialog(
            onDismiss = { deletingDate = null },
            onConfirm = {
                viewModel.deleteDay(date)
                deletingDate = null
            }
        )
    }
}

@Composable
private fun DayCard(
    dayLabel: String,
    dateText: String,
    value: Double,
    valueColor: Color,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = SimpleColors.Card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 曜日 + 日付（1行）
            Text(
                text = "$dayLabel ($dateText)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SimpleColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // 数値（色分け）
            Text(
                text = String.format(Locale.getDefault(), "%.1f", value),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )

            // 編集アイコン（青）
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.history_cd_edit),
                    tint = SimpleColors.PureBlue
                )
            }

            // 削除アイコン（赤）
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.history_cd_delete),
                    tint = SimpleColors.PureRed
                )
            }
        }
    }
}
package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import com.powder.simplebeertime.util.currentLogicalDate
import com.powder.simplebeertime.util.toLogicalDate
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
    var selectedDate by rememberSaveable { mutableStateOf(logicalToday) }

    val recordsForDate = remember(allRecords, selectedDate) {
        allRecords
            .filter { record ->
                record.timestamp.toLogicalDate(cutoffHour = 3) == selectedDate
            }
            .sortedBy { it.timestamp }
    }

    val intervalItems = remember(recordsForDate) {
        buildIntervalItems(recordsForDate.map { it.timestamp })
    }

    val intervalsMillis = intervalItems.mapNotNull { it.intervalMillis }
    val totalCount = recordsForDate.size
    val avgIntervalMillis = if (intervalsMillis.isNotEmpty()) {
        intervalsMillis.sum() / intervalsMillis.size
    } else {
        null
    }
    val longestIntervalMillis = intervalsMillis.maxOrNull()

    var itemToDelete by remember { mutableStateOf<IntervalItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        DateBar(
            selectedDate = selectedDate,
            logicalToday = logicalToday,
            onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
            onNextDay = {
                if (selectedDate.isBefore(logicalToday)) {
                    selectedDate = selectedDate.plusDays(1)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SummarySection(
            totalCount = totalCount,
            avgIntervalMillis = avgIntervalMillis,
            longestIntervalMillis = longestIntervalMillis
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(intervalItems) { item ->
                IntervalCardSimple(
                    item = item,
                    onDeleteClick = { itemToDelete = item }
                )
            }
        }
    }

    if (itemToDelete != null) {
        DeleteIntervalDialog(
            onDismiss = { itemToDelete = null },
            onConfirmDelete = {
                val target = itemToDelete
                if (target != null) {
                    viewModel.deleteRecordByTimestamp(target.currentTime)
                }
                itemToDelete = null
            }
        )
    }
}

@Composable
private fun DateBar(
    selectedDate: LocalDate,
    logicalToday: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLocale = Locale.getDefault()
    val formatter = remember(currentLocale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(
            currentLocale,
            "yyyyMMMddEEE"
        )
        DateTimeFormatter.ofPattern(pattern, currentLocale)
    }

    val dateText = remember(selectedDate, formatter) {
        selectedDate.format(formatter)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.history_cd_previous_day),
                tint = SimpleColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = dateText,
            color = SimpleColors.TextPrimary
        )

        Spacer(modifier = Modifier.width(16.dp))

        val isNextEnabled = selectedDate.isBefore(logicalToday)
        IconButton(
            onClick = { if (isNextEnabled) onNextDay() },
            enabled = isNextEnabled
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.history_cd_next_day),
                tint = SimpleColors.TextPrimary
            )
        }
    }
}

@Composable
private fun SummarySection(
    totalCount: Int,
    avgIntervalMillis: Long?,
    longestIntervalMillis: Long?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.history_summary_title),
            color = SimpleColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                id = R.string.history_summary_total,
                totalCount
            ),
            color = SimpleColors.TextPrimary
        )

        if (avgIntervalMillis != null && longestIntervalMillis != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = R.string.history_summary_avg,
                    formatIntervalHoursMinutes(avgIntervalMillis)
                ),
                color = SimpleColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = R.string.history_summary_longest,
                    formatIntervalHoursMinutes(longestIntervalMillis)
                ),
                color = SimpleColors.TextPrimary
            )
        }
    }
}

@Composable
private fun IntervalCardSimple(
    item: IntervalItem,
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
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = stringResource(
                    id = R.string.history_item_title,
                    item.index
                ),
                modifier = Modifier.weight(1f),
                color = SimpleColors.TextPrimary
            )

            val intervalText = if (item.intervalMillis == null) {
                stringResource(R.string.history_item_first)
            } else {
                stringResource(
                    id = R.string.history_item_interval,
                    formatIntervalHoursMinutes(item.intervalMillis)
                )
            }

            Text(
                text = intervalText,
                modifier = Modifier.weight(1f),
                color = SimpleColors.TextPrimary
            )

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.history_cd_delete),
                    tint = SimpleColors.PureRed
                )
            }
        }
    }
}

@Composable
private fun DeleteIntervalDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.DialogBackground,
        titleContentColor = SimpleColors.TextPrimary,
        textContentColor = SimpleColors.TextPrimary,
        title = {
            Text(
                text = stringResource(id = R.string.history_delete_title)
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.history_delete_message)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmDelete()
                }
            ) {
                Text(
                    text = stringResource(id = R.string.history_delete_yes),
                    color = SimpleColors.PureRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.history_delete_no),
                    color = SimpleColors.TextPrimary
                )
            }
        }
    )
}

private fun buildIntervalItems(
    timestamps: List<Long>
): List<IntervalItem> {
    if (timestamps.isEmpty()) return emptyList()

    val sorted = timestamps.sorted()
    val result = mutableListOf<IntervalItem>()

    for (i in sorted.indices) {
        val current = sorted[i]
        val previous = if (i == 0) null else sorted[i - 1]
        val interval = if (previous == null) null else (current - previous)

        result.add(
            IntervalItem(
                index = i + 1,
                previousTime = previous,
                currentTime = current,
                intervalMillis = interval
            )
        )
    }

    return result
}

private fun formatIntervalHoursMinutes(millis: Long): String {
    if (millis <= 0L) return "--"

    val totalMinutes = millis / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
    } else {
        String.format(Locale.getDefault(), "%dmin", minutes)
    }
}

private data class IntervalItem(
    val index: Int,
    val previousTime: Long?,
    val currentTime: Long,
    val intervalMillis: Long?
)
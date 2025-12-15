package com.powder.simplebeertime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powder.simplebeertime.data.repository.BeerRepository
import com.powder.simplebeertime.data.entity.BeerRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.powder.simplebeertime.util.currentLogicalDate
import com.powder.simplebeertime.util.toLogicalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.time.LocalDate

data class TodayStats(
    val count: Double = 0.0,
    val cost: Double = 0.0
)

data class WeekStats(
    val count: Double = 0.0,
    val avgPerDay: Double = 0.0,
    val costTotal: Double = 0.0,
    val costAvgPerDay: Double = 0.0
)

data class MonthlyTotal(
    val month: Int,
    val totalBeers: Double
)

data class WeeklyIntervalPoint(
    val year: Int,
    val month: Int,
    val weekOfMonth: Int,
    val averageIntervalMillis: Long
)

private data class WeekKey(
    val weekBasedYear: Int,
    val weekOfYear: Int
)

class BeerViewModel(
    private val repository: BeerRepository
) : ViewModel() {

    private val pricePerBeer = 500 // 円/杯 TODO: Settings画面で変更可能に

    val allRecords: StateFlow<List<BeerRecord>> =
        repository.getAllRecords()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val todayStats: StateFlow<TodayStats> =
        allRecords
            .map { list ->
                val today = currentLogicalDate()
                val todayRecords = list.filter { record ->
                    record.timestamp.toLogicalDate() == today
                }
                val count = todayRecords.size.toDouble()
                TodayStats(
                    count = count,
                    cost = count * pricePerBeer
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                TodayStats()
            )

    val weekStats: StateFlow<WeekStats> =
        allRecords
            .map { list ->
                val today = currentLogicalDate()
                val monday = today.with(DayOfWeek.MONDAY)
                val sunday = monday.plusDays(6)

                val weekRecords = list.filter { record ->
                    val d = record.timestamp.toLogicalDate()
                    !d.isBefore(monday) && !d.isAfter(sunday)
                }

                val count = weekRecords.size.toDouble()
                val daysPassed = ChronoUnit.DAYS.between(monday, today) + 1

                val avgPerDay =
                    if (daysPassed > 0) count / daysPassed.toDouble() else 0.0
                val costTotal = count * pricePerBeer
                val costAvgPerDay =
                    if (daysPassed > 0) costTotal / daysPassed.toDouble() else 0.0

                WeekStats(
                    count = count,
                    avgPerDay = avgPerDay,
                    costTotal = costTotal,
                    costAvgPerDay = costAvgPerDay
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                WeekStats()
            )

    private val _latestRecord = MutableStateFlow<BeerRecord?>(null)
    val latestRecord: StateFlow<BeerRecord?> = _latestRecord

    val todayCount: StateFlow<Double> =
        allRecords
            .map { list ->
                val today = currentLogicalDate()
                list.count { record ->
                    record.timestamp.toLogicalDate() == today
                }.toDouble()
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0.0
            )

    init {
        viewModelScope.launch {
            _latestRecord.value = repository.getLatestRecord()
        }
    }

    fun insertBeer() {
        viewModelScope.launch {
            repository.insertRecord()
            _latestRecord.value = repository.getLatestRecord()
        }
    }

    fun deleteLatestBeer() {
        viewModelScope.launch {
            _latestRecord.value?.id?.let { id ->
                repository.deleteRecord(id)
                _latestRecord.value = repository.getLatestRecord()
            }
        }
    }

    suspend fun getRecordsBetween(start: Long, end: Long): List<BeerRecord> {
        return repository.getRecordsBetween(start, end)
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            repository.deleteAllRecords()
            _latestRecord.value = null
        }
    }

    fun deleteRecordByTimestamp(timestamp: Long) {
        viewModelScope.launch {
            repository.deleteRecordByTimestamp(timestamp)
        }
    }

    private companion object {
        const val MAX_INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000L
    }

    fun getMonthlyTotalsForYear(
        year: Int,
        cutoffHour: Int = 3
    ): List<MonthlyTotal> {
        val base = (1..12).associateWith { 0.0 }.toMutableMap()

        allRecords.value.forEach { record ->
            val logicalDate = record.timestamp.toLogicalDate(cutoffHour)
            if (logicalDate.year == year) {
                val m = logicalDate.monthValue
                base[m] = (base[m] ?: 0.0) + 1.0
            }
        }

        return (1..12).map { m ->
            MonthlyTotal(month = m, totalBeers = base[m] ?: 0.0)
        }
    }

    fun getWeeklyIntervalPointsForWeekBasedYear(
        weekBasedYear: Int,
        cutoffHour: Int = 3,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<WeeklyIntervalPoint> {

        val grouped: Map<WeekKey, List<Long>> =
            allRecords.value
                .groupBy { record ->
                    val logicalDate = record.timestamp.toLogicalDate(cutoffHour)
                    weekKeyFromDate(logicalDate)
                }
                .mapValues { (_, list) -> list.map { it.timestamp }.sorted() }

        val allWeekKeys = enumerateWeekKeysForWeekBasedYear(weekBasedYear)

        val wf = WeekFields.ISO

        return allWeekKeys.map { key ->
            val timestamps = grouped[key].orEmpty()

            val avgMillis = when {
                timestamps.size >= 2 -> {
                    val intervals = timestamps.zipWithNext { a, b ->
                        val diff = b - a
                        if (diff > 0) diff else 0L
                    }.filter { it > 0L }

                    if (intervals.isNotEmpty()) {
                        intervals.average().toLong().coerceAtMost(MAX_INTERVAL_MILLIS)
                    } else {
                        MAX_INTERVAL_MILLIS
                    }
                }

                timestamps.size == 1 -> {
                    val only = timestamps.first()
                    val monday = mondayOfWeek(only.toLogicalDate(cutoffHour))
                    val nextWeekStart = monday.plusWeeks(7).atTime(3, 0)
                    val nextWeekStartMillis = localDateTimeToMillis(nextWeekStart, zoneId)

                    val diff = nextWeekStartMillis - only
                    diff.coerceAtLeast(0L).coerceAtMost(MAX_INTERVAL_MILLIS)
                }

                else -> {
                    MAX_INTERVAL_MILLIS
                }
            }

            val repMonday = mondayFromWeekKey(key)
            WeeklyIntervalPoint(
                year = key.weekBasedYear,
                month = repMonday.monthValue,
                weekOfMonth = repMonday.get(wf.weekOfMonth()),
                averageIntervalMillis = avgMillis
            )
        }.sortedWith(
            compareBy<WeeklyIntervalPoint> { it.year }
                .thenBy { it.month }
                .thenBy { it.weekOfMonth }
        )
    }

    private fun weekKeyFromDate(date: LocalDate): WeekKey {
        val wf = WeekFields.ISO
        return WeekKey(
            weekBasedYear = date.get(wf.weekBasedYear()),
            weekOfYear = date.get(wf.weekOfWeekBasedYear())
        )
    }

    private fun enumerateWeekKeysForWeekBasedYear(targetWeekBasedYear: Int): List<WeekKey> {
        val wf = WeekFields.ISO

        val anchor = LocalDate.of(targetWeekBasedYear, 1, 4)

        var cur = anchor
            .with(wf.weekOfWeekBasedYear(), 1)
            .with(wf.dayOfWeek(), 1)

        val keys = mutableListOf<WeekKey>()
        while (true) {
            val key = weekKeyFromDate(cur)
            if (key.weekBasedYear != targetWeekBasedYear && keys.isNotEmpty()) break
            if (key.weekBasedYear == targetWeekBasedYear) {
                keys.add(key)
            }
            cur = cur.plusWeeks(1)
        }
        return keys.distinct()
    }

    private fun mondayOfWeek(date: LocalDate): LocalDate {
        return date.with(DayOfWeek.MONDAY)
    }

    private fun mondayFromWeekKey(key: WeekKey): LocalDate {
        val wf = WeekFields.ISO
        return LocalDate.of(key.weekBasedYear, 1, 4)
            .with(wf.weekOfWeekBasedYear(), key.weekOfYear.toLong())
            .with(wf.dayOfWeek(), 1)
    }

    private fun localDateTimeToMillis(ldt: LocalDateTime, zoneId: ZoneId): Long {
        return ldt.atZone(zoneId).toInstant().toEpochMilli()
    }
}
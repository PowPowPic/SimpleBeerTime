package com.powder.simplebeertime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powder.simplebeertime.data.entity.BeerRecord
import com.powder.simplebeertime.data.repository.BeerRepository
import com.powder.simplebeertime.util.toLogicalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class BeerViewModel(private val repository: BeerRepository) : ViewModel() {

    val allRecords: StateFlow<List<BeerRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val latestRecord: StateFlow<BeerRecord?> = repository.latestRecord
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // 今日の統計（3時ルール適用）
    data class TodayStats(val count: Double = 0.0, val firstTime: Long? = null)

    private val _todayStats = MutableStateFlow(TodayStats())
    val todayStats: StateFlow<TodayStats> = _todayStats.asStateFlow()

    // 今週の統計
    data class WeekStats(val count: Double = 0.0, val avgPerDay: Double = 0.0)

    private val _weekStats = MutableStateFlow(WeekStats())
    val weekStats: StateFlow<WeekStats> = _weekStats.asStateFlow()

    init {
        viewModelScope.launch {
            allRecords.collect { records ->
                updateTodayStats(records)
                updateWeekStats(records)
            }
        }
    }

    private fun updateTodayStats(records: List<BeerRecord>) {
        val today = LocalDate.now()
        val todayRecords = records.filter { record ->
            record.timestamp.toLogicalDate(cutoffHour = 3) == today
        }
        val totalAmount = todayRecords.sumOf { it.amount }
        val firstTime = todayRecords.minByOrNull { it.timestamp }?.timestamp

        _todayStats.value = TodayStats(count = totalAmount, firstTime = firstTime)
    }

    private fun updateWeekStats(records: List<BeerRecord>) {
        val today = LocalDate.now()
        val startOfWeek = today.with(DayOfWeek.MONDAY)

        val weekRecords = records.filter { record ->
            val recordDate = record.timestamp.toLogicalDate(cutoffHour = 3)
            !recordDate.isBefore(startOfWeek) && !recordDate.isAfter(today)
        }

        val totalAmount = weekRecords.sumOf { it.amount }
        val daysPassed = (today.toEpochDay() - startOfWeek.toEpochDay() + 1).toInt()
        val avg = if (daysPassed > 0) totalAmount / daysPassed else 0.0

        _weekStats.value = WeekStats(count = totalAmount, avgPerDay = avg)
    }

    fun insertBeer(amount: Double = 1.0) {
        viewModelScope.launch {
            repository.insert(
                BeerRecord(
                    timestamp = System.currentTimeMillis(),
                    amount = amount
                )
            )
        }
    }

    fun deleteLatestBeer() {
        viewModelScope.launch {
            latestRecord.value?.let { record ->
                repository.deleteById(record.id)
            }
        }
    }

    fun deleteRecordByTimestamp(timestamp: Long) {
        viewModelScope.launch {
            repository.deleteByTimestamp(timestamp)
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    // グラフ用：月別集計
    data class MonthlyTotal(val month: Int, val totalBeers: Double)

    fun getMonthlyTotalsForYear(year: Int): List<MonthlyTotal> {
        val records = allRecords.value
        val result = mutableMapOf<Int, Double>()

        for (month in 1..12) {
            result[month] = 0.0
        }

        records.forEach { record ->
            val date = record.timestamp.toLogicalDate(cutoffHour = 3)
            if (date.year == year) {
                val month = date.monthValue
                result[month] = (result[month] ?: 0.0) + record.amount
            }
        }

        return result.map { (month, total) -> MonthlyTotal(month, total) }
    }
}
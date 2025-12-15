package com.powder.simplebeertime.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// 「その時刻が属するロジカル日付」を返す（深夜3時で区切る）
fun Long.toLogicalDate(cutoffHour: Int = 3): LocalDate {
    val instant = Instant.ofEpochMilli(this)
    val zone = ZoneId.systemDefault()
    val zonedDateTime = instant.atZone(zone)

    val baseDate = zonedDateTime.toLocalDate()
    val hour = zonedDateTime.hour

    return if (hour < cutoffHour) {
        baseDate.minusDays(1)
    } else {
        baseDate
    }
}

// 「今」が属するロジカル日付
fun currentLogicalDate(cutoffHour: Int = 3): LocalDate {
    return System.currentTimeMillis().toLogicalDate(cutoffHour)
}
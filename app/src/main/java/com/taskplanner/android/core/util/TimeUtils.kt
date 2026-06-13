package com.taskplanner.android.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object TimeUtils {
    fun startOfDayMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun localDateFromMillis(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    }

    fun localTimeFromMillis(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime {
        return Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
    }

    fun millisFromLocalTime(time: LocalTime, forDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val dateTime = LocalDateTime.of(forDate, time)
        return dateTime.atZone(zoneId).toInstant().toEpochMilli()
    }
}


package com.madebykunal.doon.lib

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

typealias DateStr = String

object Dates {
    private val formatters = HashMap<String, DateTimeFormatter>()
    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun today(): DateStr = LocalDate.now().toString()

    fun parse(date: DateStr): LocalDate = LocalDate.parse(date)

    fun isDateStr(value: String?): Boolean =
        value != null && datePattern.matches(value) && runCatching { LocalDate.parse(value) }.isSuccess

    fun dayNumber(date: DateStr): Long = parse(date).toEpochDay()

    fun fromDayNumber(n: Long): DateStr = LocalDate.ofEpochDay(n).toString()

    fun addDays(date: DateStr, days: Int): DateStr = parse(date).plusDays(days.toLong()).toString()

    fun diffDays(a: DateStr, b: DateStr): Int = (dayNumber(b) - dayNumber(a)).toInt()

    fun addMonths(date: DateStr, months: Int): DateStr = parse(date).plusMonths(months.toLong()).toString()

    fun makeDate(year: Int, month: Int, day: Int): DateStr = LocalDate.of(year, month, day).toString()

    fun year(date: DateStr): Int = date.substring(0, 4).toInt()
    fun month(date: DateStr): Int = date.substring(5, 7).toInt()
    fun day(date: DateStr): Int = date.substring(8, 10).toInt()

    fun daysInMonth(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

    fun isLeapYear(year: Int): Boolean = java.time.Year.isLeap(year.toLong())

    fun weekdayOf(date: DateStr): Int = parse(date).dayOfWeek.value % 7

    fun startOfWeek(date: DateStr, weekStartsOn: Int): DateStr =
        addDays(date, -((weekdayOf(date) - weekStartsOn + 7) % 7))

    fun weekDays(weekStart: DateStr): List<DateStr> = (0 until 7).map { addDays(weekStart, it) }

    fun monthGrid(year: Int, month: Int, weekStartsOn: Int): List<DateStr> {
        val first = startOfWeek(makeDate(year, month, 1), weekStartsOn)
        return (0 until 42).map { addDays(first, it) }
    }

    fun format(date: DateStr, pattern: String): String = parse(date).format(formatter(pattern))

    fun formatMillis(ms: Long, pattern: String): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(formatter(pattern))

    fun toMillis(date: DateStr, hour: Int = 12, minute: Int = 0): Long =
        parse(date).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun dateOfMillis(ms: Long): DateStr =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    fun msUntilNextMidnight(now: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val next = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone)
        return next.toInstant().toEpochMilli() - now
    }

    fun localeWeekStart(): Int = WeekFields.of(Locale.getDefault()).firstDayOfWeek.value % 7

    private fun formatter(pattern: String): DateTimeFormatter =
        formatters.getOrPut(pattern) { DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH) }
}

fun parseTime(time: String?, fallbackHour: Int = 9): Pair<Int, Int> {
    val parts = time?.split(":")?.mapNotNull { it.toIntOrNull() }.orEmpty()
    return (parts.getOrNull(0) ?: fallbackHour) to (parts.getOrNull(1) ?: 0)
}

fun pad2(n: Int): String = n.toString().padStart(2, '0')

package com.madebykunal.doon.lib

import kotlin.math.ceil
import kotlin.math.floor

data class CountdownDates(val kind: String, val targetDate: DateStr, val birthYear: Int? = null)

data class CountdownDisplay(val value: String, val unit: String, val numeric: Boolean)

data class DotGrid(val total: Int, val filled: Int, val unit: String, val elapsedDays: Int, val remainingDays: Int)

object CountdownMath {
    val alerts = listOf(0 to "On the day", 1 to "1 day before", 7 to "1 week before")

    private fun yearlyOn(target: DateStr, year: Int): DateStr {
        val month = Dates.month(target)
        val day = Dates.day(target)
        val safeDay = if (month == 2 && day == 29 && !Dates.isLeapYear(year)) 28 else day
        return Dates.makeDate(year, month, safeDay)
    }

    private fun monthlyOn(target: DateStr, year: Int, month: Int): DateStr =
        Dates.makeDate(year, month, minOf(Dates.day(target), Dates.daysInMonth(year, month)))

    fun nextOccurrence(item: CountdownDates, today: DateStr): DateStr {
        val year = Dates.year(today)
        val month = Dates.month(today)
        return when (item.kind) {
            "yearly" -> {
                if (item.targetDate > today) return item.targetDate
                val thisYear = yearlyOn(item.targetDate, year)
                if (thisYear >= today) thisYear else yearlyOn(item.targetDate, year + 1)
            }
            "monthly" -> {
                if (item.targetDate > today) return item.targetDate
                val thisMonth = monthlyOn(item.targetDate, year, month)
                if (thisMonth >= today) return thisMonth
                val next = Dates.addMonths(Dates.makeDate(year, month, 1), 1)
                monthlyOn(item.targetDate, Dates.year(next), Dates.month(next))
            }
            else -> item.targetDate
        }
    }

    fun previousOccurrence(item: CountdownDates, today: DateStr): DateStr? {
        val next = nextOccurrence(item, today)
        return when (item.kind) {
            "yearly" -> yearlyOn(item.targetDate, Dates.year(next) - 1)
            "monthly" -> {
                val previous = Dates.addMonths(Dates.makeDate(Dates.year(next), Dates.month(next), 1), -1)
                monthlyOn(item.targetDate, Dates.year(previous), Dates.month(previous))
            }
            else -> null
        }
    }

    fun daysUntil(item: CountdownDates, today: DateStr): Int = Dates.diffDays(today, nextOccurrence(item, today))

    fun turningAge(item: CountdownDates, today: DateStr): Int? {
        if (item.kind != "yearly") return null
        val began = item.birthYear ?: Dates.year(item.targetDate)
        val age = Dates.year(nextOccurrence(item, today)) - began
        return if (age > 0) age else null
    }

    private fun ageInYears(item: CountdownDates, today: DateStr): Int? {
        if (item.kind == "yearly") return turningAge(item, today)
        if (item.kind != "since" || item.targetDate > today) return null
        val hadBirthday = Dates.month(today) > Dates.month(item.targetDate) ||
            (Dates.month(today) == Dates.month(item.targetDate) && Dates.day(today) >= Dates.day(item.targetDate))
        val years = Dates.year(today) - Dates.year(item.targetDate) - if (hadBirthday) 0 else 1
        return if (years > 0) years else null
    }

    fun titleWithAge(item: CountdownDates, title: String, today: DateStr): String {
        val age = ageInYears(item, today) ?: return title
        return "$title ($age ${if (age == 1) "yr" else "yrs"})"
    }

    fun isPast(item: CountdownDates, today: DateStr): Boolean = item.kind == "until" && item.targetDate < today

    fun display(item: CountdownDates, today: DateStr): CountdownDisplay {
        if (item.kind == "since") {
            val days = Dates.diffDays(item.targetDate, today)
            if (days < 0) {
                val left = -days
                return CountdownDisplay(left.toString(), if (left == 1) "day to go" else "days to go", true)
            }
            return CountdownDisplay((days + 1).toString(), if (days == 0) "first day" else "days", true)
        }
        val days = daysUntil(item, today)
        if (days == 0) return CountdownDisplay("Today 🎉", "", false)
        if (days == 1) return CountdownDisplay("Tomorrow", "", false)
        if (days < 0) {
            val ago = -days
            return CountdownDisplay(ago.toString(), if (ago == 1) "day ago" else "days ago", true)
        }
        return CountdownDisplay(days.toString(), "days left", true)
    }

    fun dateLine(item: CountdownDates, today: DateStr): String {
        if (item.kind == "since") return "Since ${Dates.format(item.targetDate, "d MMM yyyy")}"
        val next = nextOccurrence(item, today)
        val sameYear = Dates.year(next) == Dates.year(today)
        return Dates.format(next, if (sameYear) "EEE, d MMM" else "EEE, d MMM yyyy")
    }

    fun dotGrid(item: CountdownDates, createdOn: DateStr, today: DateStr): DotGrid {
        val start: DateStr
        val end: DateStr
        if (item.kind == "since") {
            start = item.targetDate
            end = if (today > item.targetDate) today else item.targetDate
        } else {
            end = nextOccurrence(item, today)
            val previous = previousOccurrence(item, today)
            var s = if (previous != null && previous > createdOn) previous else createdOn
            if (s > end) s = end
            start = s
        }
        val span = Dates.diffDays(start, end)
        val elapsed = minOf(span, maxOf(0, Dates.diffDays(start, today)))
        val unit = if (span <= 400) "day" else if (span <= 2800) "week" else "month"
        val size = when (unit) {
            "day" -> 1.0
            "week" -> 7.0
            else -> 30.4375
        }
        val total = maxOf(1, ceil(span / size).toInt())
        return DotGrid(total, minOf(total, floor(elapsed / size).toInt()), unit, elapsed, span - elapsed)
    }

    fun <T> sort(items: List<T>, today: DateStr, dates: (T) -> CountdownDates): List<T> =
        items.sortedBy { val d = dates(it); if (d.kind == "since") Int.MAX_VALUE else daysUntil(d, today) }
}

package com.madebykunal.doon.lib

import org.json.JSONArray
import org.json.JSONObject

data class RepeatRule(
    val freq: String,
    val interval: Int,
    val byWeekday: List<Int>? = null,
    val byMonthDay: Int? = null,
) {
    fun toJson(): String {
        val o = JSONObject()
        o.put("freq", freq)
        o.put("interval", interval)
        if (byWeekday != null) o.put("by_weekday", JSONArray(byWeekday))
        if (byMonthDay != null) o.put("by_month_day", byMonthDay)
        return o.toString()
    }

    companion object {
        fun fromJson(json: String): RepeatRule? = runCatching {
            val o = JSONObject(json)
            val days = o.optJSONArray("by_weekday")?.let { a -> (0 until a.length()).map { a.getInt(it) } }
            RepeatRule(
                freq = o.getString("freq"),
                interval = o.optInt("interval", 1),
                byWeekday = days,
                byMonthDay = if (o.has("by_month_day")) o.getInt("by_month_day") else null,
            )
        }.getOrNull()
    }
}

object Recurrence {
    private val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    fun occursOn(rule: RepeatRule, start: DateStr, date: DateStr): Boolean {
        if (date < start) return false
        val interval = maxOf(1, rule.interval)
        return when (rule.freq) {
            "daily" -> (Dates.dayNumber(date) - Dates.dayNumber(start)) % interval == 0L
            "weekly" -> {
                val weekdays = rule.byWeekday?.takeIf { it.isNotEmpty() } ?: listOf(Dates.weekdayOf(start))
                if (Dates.weekdayOf(date) !in weekdays) return false
                val weeks = (Dates.dayNumber(Dates.startOfWeek(date, 0)) - Dates.dayNumber(Dates.startOfWeek(start, 0))) / 7
                weeks % interval == 0L
            }
            "monthly" -> {
                val months = (Dates.year(date) - Dates.year(start)) * 12 + (Dates.month(date) - Dates.month(start))
                if (months % interval != 0) return false
                val wanted = rule.byMonthDay ?: Dates.day(start)
                Dates.day(date) == minOf(wanted, Dates.daysInMonth(Dates.year(date), Dates.month(date)))
            }
            "yearly" -> {
                if (Dates.month(start) != Dates.month(date) || (Dates.year(date) - Dates.year(start)) % interval != 0) return false
                Dates.day(date) == minOf(Dates.day(start), Dates.daysInMonth(Dates.year(date), Dates.month(date)))
            }
            else -> false
        }
    }

    fun expand(rule: RepeatRule, start: DateStr, end: DateStr?, from: DateStr, to: DateStr): List<DateStr> {
        val first = if (from > start) from else start
        val last = if (end != null && end < to) end else to
        val dates = mutableListOf<DateStr>()
        var d = first
        while (d <= last) {
            if (occursOn(rule, start, d)) dates.add(d)
            d = Dates.addDays(d, 1)
        }
        return dates
    }

    private fun ordinal(n: Int): String {
        val teen = n % 100
        if (teen in 11..13) return "${n}th"
        return n.toString() + (listOf("th", "st", "nd", "rd").getOrNull(n % 10) ?: "th")
    }

    private fun listDays(days: List<Int>): String {
        val names = days.sorted().map { dayNames[it] }
        if (names.size == 1) return names[0]
        return names.dropLast(1).joinToString(", ") + " and " + names.last()
    }

    fun describe(rule: RepeatRule): String {
        val n = maxOf(1, rule.interval)
        return when (rule.freq) {
            "daily" -> when (n) {
                1 -> "Repeats daily"
                2 -> "Repeats every other day"
                else -> "Repeats every $n days"
            }
            "weekly" -> {
                val days = rule.byWeekday.orEmpty()
                val sorted = days.sorted().joinToString(",")
                if (n == 1 && sorted == "1,2,3,4,5") return "Repeats every weekday"
                if (n == 1 && sorted == "0,6") return "Repeats every weekend"
                val on = if (days.isNotEmpty()) " on ${listDays(days)}" else ""
                when (n) {
                    1 -> "Repeats weekly$on"
                    2 -> "Repeats every other week$on"
                    else -> "Repeats every $n weeks$on"
                }
            }
            "monthly" -> {
                val on = rule.byMonthDay?.let { " on the ${ordinal(it)}" } ?: ""
                if (n == 1) "Repeats monthly$on" else "Repeats every $n months$on"
            }
            else -> if (n == 1) "Repeats yearly" else "Repeats every $n years"
        }
    }
}

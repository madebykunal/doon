package com.madebykunal.doon.lib

data class DayLog(val date: DateStr, val count: Int)

data class Schedule(val kind: String, val target: Int, val days: Int, val startDate: DateStr)

enum class DayState { Done, Partial, Missed, Off, Open, Clean, Slip, None }

object HabitMath {
    const val EVERY_DAY = 0b1111111

    fun isScheduled(days: Int, date: DateStr): Boolean = (days and (1 shl Dates.weekdayOf(date))) != 0

    fun countOn(logs: List<DayLog>, date: DateStr): Int = logs.firstOrNull { it.date == date }?.count ?: 0

    private fun doneDates(habit: Schedule, logs: List<DayLog>): Set<DateStr> =
        logs.filter { it.count >= habit.target }.map { it.date }.toHashSet()

    private fun lastSlip(habit: Schedule, logs: List<DayLog>, onOrBefore: DateStr): DateStr? =
        logs.firstOrNull { it.date <= onOrBefore && it.date >= habit.startDate }?.date

    fun currentStreak(habit: Schedule, logs: List<DayLog>, today: DateStr): Int {
        if (habit.kind == "quit") return maxOf(0, Dates.diffDays(lastSlip(habit, logs, today) ?: habit.startDate, today))
        val done = doneDates(habit, logs)
        var streak = 0
        var date = if (today in done) today else Dates.addDays(today, -1)
        while (date >= habit.startDate) {
            if (isScheduled(habit.days, date)) {
                if (date !in done) break
                streak++
            }
            date = Dates.addDays(date, -1)
        }
        return streak
    }

    fun bestStreak(habit: Schedule, logs: List<DayLog>, today: DateStr): Int {
        if (habit.kind == "quit") {
            val slips = logs.map { it.date }.filter { it >= habit.startDate && it <= today }.sorted()
            var best = 0
            var from = habit.startDate
            for (slip in slips + today) {
                best = maxOf(best, Dates.diffDays(from, slip))
                from = slip
            }
            return best
        }
        val done = doneDates(habit, logs)
        var best = 0
        var run = 0
        var date = habit.startDate
        while (date <= today) {
            if (isScheduled(habit.days, date)) {
                if (date in done) {
                    run++
                    best = maxOf(best, run)
                } else if (date != today) run = 0
            }
            date = Dates.addDays(date, 1)
        }
        return best
    }

    fun dayState(habit: Schedule, logs: List<DayLog>, date: DateStr, today: DateStr): DayState {
        if (date > today || date < habit.startDate) return DayState.None
        val count = countOn(logs, date)
        if (habit.kind == "quit") return if (count > 0) DayState.Slip else DayState.Clean
        if (count >= habit.target) return DayState.Done
        if (!isScheduled(habit.days, date)) return DayState.Off
        if (count > 0) return DayState.Partial
        return if (date == today) DayState.Open else DayState.Missed
    }

    fun streakUnit(habit: Schedule, streak: Int): String =
        if (habit.kind == "quit") (if (streak == 1) "day free" else "days free") else "day streak"

    fun scheduleLine(habit: Schedule, weekStartsOn: Int): String {
        if (habit.kind == "quit") return "Free since ${Dates.format(habit.startDate, "d MMM yyyy")}"
        val times = if (habit.target == 1) "" else "${habit.target} times a day, "
        val line = if (habit.days == EVERY_DAY) "${times}every day" else {
            val names = (0 until 7).map { (weekStartsOn + it) % 7 }
                .filter { habit.days and (1 shl it) != 0 }
                .map { Dates.format(Dates.addDays("2026-11-01", it), "EEE") }
            times + names.joinToString(", ")
        }
        return line.replaceFirstChar { it.uppercase() }
    }

    fun nextCount(habit: Schedule, current: Int): Int {
        if (habit.kind == "quit") return if (current > 0) 0 else 1
        return if (current >= habit.target) 0 else current + 1
    }
}

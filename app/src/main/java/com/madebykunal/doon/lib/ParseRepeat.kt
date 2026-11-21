package com.madebykunal.doon.lib

data class ParsedRepeat(val text: String, val rule: RepeatRule, val label: String)

object ParseRepeat {
    private val weekdayWords = mapOf(
        "sun" to 0, "sunday" to 0,
        "mon" to 1, "monday" to 1,
        "tue" to 2, "tues" to 2, "tuesday" to 2,
        "wed" to 3, "weds" to 3, "wednesday" to 3,
        "thu" to 4, "thur" to 4, "thurs" to 4, "thursday" to 4,
        "fri" to 5, "friday" to 5,
        "sat" to 6, "saturday" to 6,
    )

    private const val DAY = "(?:sun(?:day)?|mon(?:day)?|tue(?:s|sday)?|wed(?:s|nesday)?|thu(?:r|rs|rsday)?|fri(?:day)?|sat(?:urday)?)s?"
    private const val DAY_LIST = "$DAY(?:\\s*(?:,|and|&|,\\s*and)\\s*$DAY)*"
    private const val ORD = "(\\d{1,2})(?:st|nd|rd|th)"
    private val trailing = Regex("[,;:–—-]+$")
    private val word = Regex("[a-z]+")

    private fun every(body: String) = Regex("(?:^|\\s)every\\s+$body\\s*$", RegexOption.IGNORE_CASE)
    private fun plain(body: String) = Regex("(?:^|\\s)$body\\s*$", RegexOption.IGNORE_CASE)

    private fun daysFrom(phrase: String): List<Int> =
        word.findAll(phrase.lowercase()).map { it.value }
            .filter { it != "and" }
            .mapNotNull { weekdayWords[it] ?: weekdayWords[it.removeSuffix("s")] }
            .distinct()
            .toList()

    private fun monthDay(raw: String?): RepeatRule? {
        val day = raw?.toIntOrNull() ?: return null
        return if (day in 1..31) RepeatRule("monthly", 1, byMonthDay = day) else null
    }

    private val matchers: List<Pair<Regex, (MatchResult) -> RepeatRule?>> = listOf(
        every("other\\s+day") to { _ -> RepeatRule("daily", 2) },
        every("other\\s+week") to { _ -> RepeatRule("weekly", 2) },
        every("day") to { _ -> RepeatRule("daily", 1) },
        plain("daily") to { _ -> RepeatRule("daily", 1) },
        every("week\\s*days?") to { _ -> RepeatRule("weekly", 1, listOf(1, 2, 3, 4, 5)) },
        every("week\\s*ends?") to { _ -> RepeatRule("weekly", 1, listOf(0, 6)) },
        every("week") to { _ -> RepeatRule("weekly", 1) },
        plain("weekly") to { _ -> RepeatRule("weekly", 1) },
        every("(\\d{1,3})\\s+(day|week|month|year)s?") to { m ->
            val interval = m.groupValues[1].toInt()
            if (interval < 1) null else {
                val freq = when (m.groupValues[2].lowercase()) {
                    "day" -> "daily"
                    "week" -> "weekly"
                    "month" -> "monthly"
                    else -> "yearly"
                }
                RepeatRule(freq, interval)
            }
        },
        every("month\\s+on\\s+(?:the\\s+)?$ORD") to { m -> monthDay(m.groupValues[1]) },
        every("month") to { _ -> RepeatRule("monthly", 1) },
        plain("monthly") to { _ -> RepeatRule("monthly", 1) },
        every("year") to { _ -> RepeatRule("yearly", 1) },
        plain("yearly") to { _ -> RepeatRule("yearly", 1) },
        every(ORD) to { m -> monthDay(m.groupValues[1]) },
        every("($DAY_LIST)") to { m ->
            val days = daysFrom(m.groupValues[1])
            if (days.isEmpty()) null else RepeatRule("weekly", 1, days)
        },
    )

    fun parse(input: String): ParsedRepeat? {
        val source = input.trimEnd()
        for ((pattern, build) in matchers) {
            val match = pattern.find(source) ?: continue
            val rule = build(match) ?: continue
            val text = source.substring(0, match.range.first).trim().replace(trailing, "").trim()
            if (text.isEmpty()) return null
            return ParsedRepeat(text, rule, Recurrence.describe(rule))
        }
        return null
    }

    fun ruleOnly(phrase: String): RepeatRule? = parse("x $phrase")?.rule
}

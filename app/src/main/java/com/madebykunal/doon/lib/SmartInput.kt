package com.madebykunal.doon.lib

data class SmartResult(
    val title: String,
    val date: DateStr?,
    val time: String?,
    val rule: RepeatRule?,
    val spans: List<IntRange>,
) {
    val found get() = date != null || time != null || rule != null

    fun summary(today: DateStr): String = listOfNotNull(
        date?.let {
            when (it) {
                today -> "Today"
                Dates.addDays(today, 1) -> "Tomorrow"
                else -> Dates.format(it, if (Dates.year(it) == Dates.year(today)) "EEE d MMM" else "EEE d MMM yyyy")
            }
        },
        time,
        rule?.let { Recurrence.describe(it).removePrefix("Repeats ").replaceFirstChar(Char::uppercase) },
    ).joinToString(" · ")
}

object SmartInput {
    private class Found {
        var date: DateStr? = null
        var time: String? = null
        var rule: RepeatRule? = null
    }

    private fun interface Matcher {
        fun apply(m: MatchResult, today: DateStr, found: Found): Boolean
    }

    private const val B = "(?<![\\p{L}\\p{N}])"
    private const val E = "(?![\\p{L}\\p{N}])"
    private const val WD = "(mon(?:day)?|tue(?:s|sday)?|wed(?:s|nesday)?|thu(?:r|rs|rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)"
    private const val WD_LIST = "$WD(?:\\s*(?:,|and|&|,\\s*and)\\s*$WD)*"
    private const val MONTH = "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"
    private const val ORD = "(?:st|nd|rd|th)"

    private val weekdays = mapOf("sun" to 0, "mon" to 1, "tue" to 2, "wed" to 3, "thu" to 4, "fri" to 5, "sat" to 6)
    private val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val ambiguousShort = setOf("sat", "sun", "wed", "mon", "mar", "may")
    private val numbers = mapOf("a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7)

    private fun rx(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    private fun weekday(word: String): Int = weekdays.getValue(word.lowercase().take(3))

    private fun upcoming(today: DateStr, weekday: Int, includeToday: Boolean): DateStr {
        val diff = (weekday - Dates.weekdayOf(today) + 7) % 7
        return Dates.addDays(today, if (diff == 0 && !includeToday) 7 else diff)
    }

    private fun safeDate(year: Int, month: Int, day: Int): DateStr? =
        if (month in 1..12 && day in 1..Dates.daysInMonth(year, month)) Dates.makeDate(year, month, day) else null

    private fun futureDate(today: DateStr, month: Int, day: Int): DateStr? {
        val year = Dates.year(today)
        val d = safeDate(year, month, day) ?: return null
        return if (d < today) safeDate(year + 1, month, day) else d
    }

    private fun setTime(found: Found, hour: Int, minute: Int): Boolean {
        if (found.time != null || hour !in 0..23 || minute !in 0..59) return false
        found.time = "${pad2(hour)}:${pad2(minute)}"
        return true
    }

    private fun setDate(found: Found, date: DateStr?): Boolean {
        if (found.date != null || date == null) return false
        found.date = date
        return true
    }

    private fun hour12(hour: Int, meridiem: String?): Int {
        val m = meridiem?.lowercase()?.replace(".", "")
        return when {
            m == null -> hour
            m.startsWith("p") -> if (hour == 12) 12 else hour + 12
            else -> if (hour == 12) 0 else hour
        }
    }

    // Matchers run in order and each claims its text, so more specific patterns must come first.
    private val matchers: List<Pair<Regex, Matcher>> = listOf(
        rx("$B(?:every|each)\\s+(?:other\\s+)?(?:\\d{1,3}\\s+)?(?:week\\s*days?|week\\s*ends?|days?|weeks?|months?|years?|$WD_LIST|\\d{1,2}$ORD)(?:\\s+on\\s+(?:the\\s+)?\\d{1,2}$ORD)?$E") to Matcher { m, _, f ->
            val rule = ParseRepeat.ruleOnly(m.value.replace(Regex("^each", RegexOption.IGNORE_CASE), "every")) ?: return@Matcher false
            if (f.rule != null) return@Matcher false
            f.rule = rule
            true
        },
        rx("$B(daily|weekly|monthly|yearly)$E") to Matcher { m, _, f ->
            if (f.rule != null) return@Matcher false
            f.rule = ParseRepeat.ruleOnly(m.groupValues[1]) ?: return@Matcher false
            true
        },
        rx("$B(?:the\\s+)?day\\s+after\\s+(?:tomorrow|tmrw?)$E") to Matcher { _, t, f -> setDate(f, Dates.addDays(t, 2)) },
        rx("$B(today|tonight|tomorrow|tmrw|tmr)$E") to Matcher { m, t, f ->
            val word = m.groupValues[1].lowercase()
            val ok = setDate(f, if (word == "today" || word == "tonight") t else Dates.addDays(t, 1))
            if (ok && word == "tonight") setTime(f, 20, 0)
            ok
        },
        rx("${B}in\\s+(\\d{1,3}|a|an|one|two|three|four|five|six|seven)\\s+(day|week|month)s?$E") to Matcher { m, t, f ->
            val n = m.groupValues[1].toIntOrNull() ?: numbers.getValue(m.groupValues[1].lowercase())
            setDate(
                f,
                when (m.groupValues[2].lowercase()) {
                    "day" -> Dates.addDays(t, n)
                    "week" -> Dates.addDays(t, n * 7)
                    else -> Dates.addMonths(t, n)
                },
            )
        },
        rx("$B(?:on\\s+)?(?:the\\s+)?(\\d{1,2})$ORD?(?:\\s+of)?\\s+$MONTH(?:\\s+(\\d{4}))?$E") to Matcher { m, t, f ->
            val month = months.indexOf(m.groupValues[2].lowercase().take(3)) + 1
            val day = m.groupValues[1].toInt()
            val year = m.groupValues[3].toIntOrNull()
            setDate(f, if (year != null) safeDate(year, month, day) else futureDate(t, month, day))
        },
        rx("$B(?:on\\s+)?$MONTH\\s+(\\d{1,2})$ORD?(?:,?\\s+(\\d{4}))?$E") to Matcher { m, t, f ->
            val month = months.indexOf(m.groupValues[1].lowercase().take(3)) + 1
            val day = m.groupValues[2].toInt()
            val year = m.groupValues[3].toIntOrNull()
            setDate(f, if (year != null) safeDate(year, month, day) else futureDate(t, month, day))
        },
        rx("$B(?:on\\s+)?(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?$E") to Matcher { m, t, f ->
            val day = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val year = m.groupValues[3].toIntOrNull()?.let { if (it < 100) it + 2000 else it }
            setDate(f, if (year != null) safeDate(year, month, day) else futureDate(t, month, day))
        },
        rx("${B}on\\s+(?:the\\s+)?(\\d{1,2})$ORD$E") to Matcher { m, t, f ->
            val day = m.groupValues[1].toInt()
            val thisMonth = safeDate(Dates.year(t), Dates.month(t), day)
            val date = if (thisMonth != null && thisMonth >= t) thisMonth else {
                val next = Dates.addMonths(Dates.makeDate(Dates.year(t), Dates.month(t), 1), 1)
                safeDate(Dates.year(next), Dates.month(next), day)
            }
            setDate(f, date)
        },
        rx("$B(?:(on|this|next|by|coming)\\s+)?$WD$E") to Matcher { m, t, f ->
            val prefix = m.groupValues[1].lowercase()
            val word = m.groupValues[2].lowercase()
            if (prefix.isEmpty() && word in ambiguousShort) return@Matcher false
            val day = weekday(word)
            val date = if (prefix == "next") Dates.addDays(Dates.startOfWeek(t, 1), 7 + (day + 6) % 7) else upcoming(t, day, includeToday = true)
            setDate(f, date)
        },
        rx("$B(?:at\\s+|@\\s*|by\\s+)?(\\d{1,2})[:.](\\d{2})\\s*(am|pm|a\\.m\\.|p\\.m\\.)?$E") to Matcher { m, _, f ->
            val raw = m.value.trimStart()
            val explicit = raw.startsWith("at", true) || raw.startsWith("@") || raw.startsWith("by", true) || raw.contains(':') || m.groupValues[3].isNotEmpty()
            if (!explicit) return@Matcher false
            setTime(f, hour12(m.groupValues[1].toInt(), m.groupValues[3].ifEmpty { null }), m.groupValues[2].toInt())
        },
        rx("$B(?:at\\s+|@\\s*|by\\s+)?(\\d{1,2})\\s*(am|pm|a\\.m\\.|p\\.m\\.)") to Matcher { m, _, f ->
            val hour = m.groupValues[1].toInt()
            if (hour !in 1..12) return@Matcher false
            setTime(f, hour12(hour, m.groupValues[2]), 0)
        },
        rx("${B}at\\s+(\\d{1,2})$E") to Matcher { m, _, f ->
            val hour = m.groupValues[1].toInt()
            setTime(f, if (hour in 1..7) hour + 12 else hour, 0)
        },
        rx("$B(?:in\\s+the\\s+|this\\s+)?(morning|noon|midday|afternoon|evening|night)$E") to Matcher { m, _, f ->
            when (m.groupValues[1].lowercase()) {
                "morning" -> setTime(f, 9, 0)
                "noon", "midday" -> setTime(f, 12, 0)
                "afternoon" -> setTime(f, 15, 0)
                "evening" -> setTime(f, 18, 0)
                else -> setTime(f, 21, 0)
            }
        },
    )

    private val dangling = Regex("^(?:on|at|by|for|this|next|and|,|-|–|—)\\s+|\\s+(?:on|at|by|for|this|next|and|,|-|–|—)$", RegexOption.IGNORE_CASE)

    fun parse(text: String, today: DateStr): SmartResult {
        val taken = BooleanArray(text.length)
        val spans = mutableListOf<IntRange>()
        val found = Found()
        for ((regex, matcher) in matchers) {
            for (m in regex.findAll(text)) {
                var range = m.range
                while (range.first < range.last && text[range.first].isWhitespace()) range = (range.first + 1)..range.last
                if (range.any { taken[it] }) continue
                if (!matcher.apply(m, today, found)) continue
                range.forEach { taken[it] = true }
                spans.add(range)
            }
        }
        val kept = StringBuilder()
        text.forEachIndexed { i, ch -> kept.append(if (taken[i]) ' ' else ch) }
        var title = kept.toString().replace(Regex("\\s+"), " ").trim()
        while (true) {
            val next = title.replace(dangling, "").trim().trim(',', ';', ':', '-', '–', '—').trim()
            if (next == title) break
            title = next
        }
        return SmartResult(title, found.date, found.time, found.rule, spans.sortedBy { it.first })
    }
}

package com.madebykunal.doon

import com.madebykunal.doon.lib.ColorMath
import com.madebykunal.doon.lib.CountdownDates
import com.madebykunal.doon.lib.CountdownMath
import com.madebykunal.doon.lib.DayLog
import com.madebykunal.doon.lib.Dates
import com.madebykunal.doon.lib.FractionalIndex
import com.madebykunal.doon.lib.HabitMath
import com.madebykunal.doon.lib.InlineNode
import com.madebykunal.doon.lib.Markdown
import com.madebykunal.doon.lib.ParseRepeat
import com.madebykunal.doon.lib.Recurrence
import com.madebykunal.doon.lib.RepeatRule
import com.madebykunal.doon.lib.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test
    fun fractionalIndexMatchesReference() {
        assertEquals("a0", FractionalIndex.between(null, null))
        assertEquals("a1", FractionalIndex.between("a0", null))
        assertEquals("Zz", FractionalIndex.between(null, "a0"))
        assertEquals("a0V", FractionalIndex.between("a0", "a1"))
        assertEquals("a1V", FractionalIndex.between("a1", "a2"))
        assertEquals("b00", FractionalIndex.between("az", null))
        assertEquals(listOf("a0", "a1", "a2"), FractionalIndex.nBetween(null, null, 3))
        val keys = FractionalIndex.nBetween("a0", "a1", 5)
        assertEquals(keys.sorted(), keys)
        assertTrue(keys.all { it > "a0" && it < "a1" })
    }

    @Test
    fun datesWorkOnLocalCalendarDays() {
        assertEquals("2024-03-01", Dates.addDays("2024-02-28", 2))
        assertEquals("2024-02-29", Dates.addMonths("2024-01-31", 1))
        assertEquals(0, Dates.weekdayOf("2026-11-01"))
        assertEquals("2026-11-02", Dates.startOfWeek("2026-11-08", 1))
        assertEquals(365, Dates.diffDays("2025-01-01", "2026-01-01"))
        assertTrue(Dates.isDateStr("2024-02-29"))
        assertFalse(Dates.isDateStr("2023-02-29"))
    }

    @Test
    fun parsesRepeatPhrases() {
        val weekly = ParseRepeat.parse("Gym every mon and thu")!!
        assertEquals("Gym", weekly.text)
        assertEquals(RepeatRule("weekly", 1, listOf(1, 4)), weekly.rule)
        assertEquals("Repeats weekly on Mon and Thu", weekly.label)
        assertEquals(RepeatRule("monthly", 1, byMonthDay = 15), ParseRepeat.parse("Rent every 15th")!!.rule)
        assertEquals(RepeatRule("weekly", 2), ParseRepeat.parse("Bins, every 2 weeks")!!.rule)
        assertEquals("Bins", ParseRepeat.parse("Bins, every 2 weeks")!!.text)
        assertEquals(RepeatRule("weekly", 1, listOf(1, 2, 3, 4, 5)), ParseRepeat.ruleOnly("every weekday"))
        assertNull(ParseRepeat.parse("every day"))
        assertNull(ParseRepeat.parse("Buy milk"))
    }

    @Test
    fun recurrenceExpands() {
        val rule = RepeatRule("weekly", 2, listOf(1))
        assertEquals(listOf("2026-11-02", "2026-11-16"), Recurrence.expand(rule, "2026-11-02", null, "2026-11-01", "2026-11-20"))
        assertTrue(Recurrence.occursOn(RepeatRule("monthly", 1), "2026-01-31", "2026-02-28"))
        assertEquals("Repeats every other day", Recurrence.describe(RepeatRule("daily", 2)))
    }

    @Test
    fun markdownIsSafe() {
        val nodes = Markdown.parseInline("**Call** [doc](javascript:alert(1)) https://example.com/x.")
        assertEquals(InlineNode.Styled(InlineNode.Style.Bold, listOf(InlineNode.Text("Call"))), nodes[0])
        assertTrue(nodes.none { it is InlineNode.Link && !Markdown.isSafeUrl(it.url) })
        assertEquals(InlineNode.Link("https://example.com/x", "https://example.com/x"), nodes.first { it is InlineNode.Link })
        assertEquals("Plan trip", Markdown.headingText("# Plan trip"))
        assertEquals("Call mum", Markdown.plainText("*Call* `mum`"))
    }

    @Test
    fun countdownsAndHabits() {
        val birthday = CountdownDates("yearly", "1990-02-29")
        assertEquals("2027-02-28", CountdownMath.nextOccurrence(birthday, "2026-11-21"))
        assertEquals(37, CountdownMath.turningAge(birthday, "2026-11-21"))
        assertEquals("Tomorrow", CountdownMath.display(CountdownDates("until", "2026-11-22"), "2026-11-21").value)
        assertEquals("1", CountdownMath.display(CountdownDates("since", "2026-11-21"), "2026-11-21").value)
        val habit = Schedule("build", 1, HabitMath.EVERY_DAY, "2026-11-01")
        val logs = listOf(DayLog("2026-11-21", 1), DayLog("2026-11-20", 1), DayLog("2026-11-18", 1))
        assertEquals(2, HabitMath.currentStreak(habit, logs, "2026-11-21"))
        assertEquals(2, HabitMath.bestStreak(habit, logs, "2026-11-21"))
        assertEquals("#1E9E9A", ColorMath.ensureContrast("#1E9E9A", "#FAF8F5", 3.0))
        assertEquals("#FFFFFF", ColorMath.readableOn("#3B5BDB"))
        assertEquals("#FFFFFF", ColorMath.readableOn("#E5484D"))
        assertEquals("#FFFFFF", ColorMath.readableOn("#F2765A"))
        assertEquals("#FFFFFF", ColorMath.readableOn("#C62F35"))
        assertEquals("#000000", ColorMath.readableOn("#E9A23B"))
        assertEquals("#000000", ColorMath.readableOn("#8FD98A"))
    }
}

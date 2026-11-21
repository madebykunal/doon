package com.madebykunal.doon

import com.madebykunal.doon.lib.RepeatRule
import com.madebykunal.doon.lib.SmartInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SmartInputTest {
    private val today = "2026-11-21"

    @Test
    fun repeatAnywhere() {
        val r = SmartInput.parse("every friday meet with Aryan", today)
        assertEquals("meet with Aryan", r.title)
        assertEquals(RepeatRule("weekly", 1, listOf(5)), r.rule)
        assertEquals(listOf(0..11), r.spans)
    }

    @Test
    fun weekdayWithPrefix() {
        val r = SmartInput.parse("on fri call mum", today)
        assertEquals("call mum", r.title)
        assertEquals("2026-11-27", r.date)
        assertEquals("2026-11-23", SmartInput.parse("next monday dentist", today).date)
    }

    @Test
    fun dayAndTimeInAnyOrder() {
        val r = SmartInput.parse("Call Aryan at 5pm tomorrow", today)
        assertEquals("Call Aryan", r.title)
        assertEquals("2026-11-22", r.date)
        assertEquals("17:00", r.time)
        val s = SmartInput.parse("tomorrow 9:30 standup", today)
        assertEquals("standup", s.title)
        assertEquals("09:30", s.time)
    }

    @Test
    fun calendarDates() {
        assertEquals("2026-12-25", SmartInput.parse("Gifts on 25 dec", today).date)
        assertEquals("2027-01-03", SmartInput.parse("Pay rent jan 3", today).date)
        assertEquals("2026-12-05", SmartInput.parse("Book 5/12", today).date)
        assertEquals("2026-12-02", SmartInput.parse("Taxes in 11 days", today).date)
        assertEquals("20:00", SmartInput.parse("Read tonight", today).time)
        assertEquals(today, SmartInput.parse("Read tonight", today).date)
    }

    @Test
    fun leavesPlainTextAlone() {
        val r = SmartInput.parse("I sat on the sun deck", today)
        assertFalse(r.found)
        assertEquals("I sat on the sun deck", r.title)
        assertNull(SmartInput.parse("Buy milk", today).date)
    }
}

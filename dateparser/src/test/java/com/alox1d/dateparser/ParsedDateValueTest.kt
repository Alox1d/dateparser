package com.alox1d.dateparser

import com.huawei.wordnote.domain.services.dateparser.ParsedDate
import com.huawei.wordnote.domain.services.dateparser.TimeList
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ParsedDateValueTest {
    private fun parsed(period: TimeList = TimeList(), maximum: TimeList = TimeList()) =
        ParsedDate(TimeList().apply {
            years = 2024; months = Calendar.FEBRUARY; dates = 29
            hours = 17; minutes = 30; seconds = 0
        }, period, maximum, "meeting", 100, arrayListOf())

    private fun inZone(zone: String, block: () -> Unit) {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun `absent recurrence is zero in every timezone`() {
        for (zone in listOf("UTC", "Europe/Moscow", "America/New_York")) inZone(zone) {
            assertEquals(zone, 0L, parsed().valueOf().period_time)
        }
    }

    @Test fun `two-day recurrence is a duration without mutating input`() = inZone("UTC") {
        val period = TimeList().apply { dates = 2 }
        val value = parsed(period).valueOf()
        assertEquals(TimeUnit.DAYS.toMillis(2), value.period_time)
        assertEquals(2, period.dates)
    }

    @Test fun `combined recurrence preserves hours minutes and seconds`() {
        for (zone in listOf("UTC", "Europe/Moscow", "America/New_York")) inZone(zone) {
            val period = TimeList().apply { hours = 5; minutes = 30; seconds = 15 }
            assertEquals(zone, 19_815_000L, parsed(period).valueOf().period_time)
        }
    }

    @Test fun `absolute dates use calendar years and zero milliseconds`() = inZone("UTC") {
        val value = parsed(maximum = TimeList().apply {
            years = 2024; months = Calendar.FEBRUARY; dates = 29
            hours = 20; minutes = 0; seconds = 0
        }).valueOf()
        assertEquals(2024, value.target_date.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, value.target_date.get(Calendar.MONTH))
        assertEquals(29, value.target_date.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, value.target_date.get(Calendar.MILLISECOND))
        val expected = Calendar.getInstance().apply {
            clear(); set(2024, Calendar.FEBRUARY, 29, 20, 0, 0)
        }
        assertEquals(expected.timeInMillis, value.max_date)
    }
    @Test fun `month and year recurrence use UTC epoch calendar units`() {
        for (zone in listOf("UTC", "Europe/Moscow", "America/New_York")) inZone(zone) {
            assertEquals(TimeUnit.DAYS.toMillis(31), parsed(TimeList().apply { months = 1 }).valueOf().period_time)
            assertEquals(TimeUnit.DAYS.toMillis(365), parsed(TimeList().apply { years = 1 }).valueOf().period_time)
        }
    }
}

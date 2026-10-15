package io.github.alox1d.dateparser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class FirstWeekdayNextMonthTest {
    private val reference = Instant.parse("2024-02-28T10:15:21Z")

    private fun one(text: String, instant: Instant = reference, zone: TimeZone = TimeZone.UTC): DateMatch {
        val results = DateParser().parse(text, instant, zone)
        assertEquals(1, results.size, text)
        return results.single()
    }

    @Test fun nextFridayAlreadyWorksAndSkipsACurrentFriday() {
        assertEquals(LocalDate(2024, 3, 1), one("next Friday").targetDate.date)
        val friday = Instant.parse("2026-07-03T10:15:21Z")
        assertEquals(LocalDateTime(2026, 7, 10, 10, 15, 21), one("next Friday", friday).targetDate)
    }

    @Test fun firstMondayPreservesReferenceClockAndReminderTitle() {
        val result = one("Pay rent on the first Monday of next month")
        assertEquals(LocalDateTime(2024, 3, 4, 10, 15, 21), result.targetDate)
        assertEquals("Pay rent", result.remainingText)
    }

    @Test fun eachWeekdayResolvesWithinTheNextCalendarMonth() {
        val days = mapOf("Monday" to 4, "Tuesday" to 5, "Wednesday" to 6, "Thursday" to 7,
            "Friday" to 1, "Saturday" to 2, "Sunday" to 3)
        for ((name, day) in days) {
            assertEquals(LocalDate(2024, 3, day), one("first $name of next month").targetDate.date, name)
        }
    }

    @Test fun explicitClockAndCaseKeepTheirExistingSemantics() {
        val result = one("Email Alice on the FIRST mOnDaY of NEXT MONTH at 5PM!")
        assertEquals(LocalDateTime(2024, 3, 4, 17, 0, 0), result.targetDate)
        assertEquals("Email Alice !", result.remainingText)
    }

    @Test fun monthEndAndYearRolloverStartAtTheFirstOfTheNextMonth() {
        assertEquals(LocalDate(2024, 2, 1), one("first Thursday of next month", Instant.parse("2024-01-31T10:15:21Z")).targetDate.date)
        assertEquals(LocalDate(2025, 1, 6), one("the first Monday of next month", Instant.parse("2024-12-31T10:15:21Z")).targetDate.date)
    }

    @Test fun nextMonthUsesTheReferenceDateInTheRequestedTimeZone() {
        val instant = Instant.parse("2026-03-01T00:30:00Z")
        assertEquals(LocalDate(2026, 4, 6), one("first Monday of next month", instant).targetDate.date)
        val local = one("first Monday of next month", instant, TimeZone.of("America/Los_Angeles"))
        assertEquals(LocalDateTime(2026, 3, 2, 16, 30, 0), local.targetDate)
        assertEquals(Instant.parse("2026-03-03T00:30:00Z"), local.instant)
    }

    @Test fun unsupportedOrdinalsAndRecurringFormsDoNotFallBackToPartialDates() {
        for (text in listOf("the second Monday of next month", "the fifth Monday of next month at 5PM",
            "the last Friday of next month", "every first Monday of next month", "each first Monday of next month")) {
            assertTrue(DateParser().parse(text, reference, TimeZone.UTC).isEmpty(), text)
        }
    }
}

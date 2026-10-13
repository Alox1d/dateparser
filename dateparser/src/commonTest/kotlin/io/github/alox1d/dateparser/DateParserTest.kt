package io.github.alox1d.dateparser

import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.*

class DateParserTest {
    private val parser = DateParser()
    private val reference = Instant.parse("2024-02-28T10:15:00Z")
    private val utc = TimeZone.UTC
    private fun parse(text: String) = parser.parse(text, reference, utc)

    @Test fun clockTimeInRussianAndEnglish() {
        for (text in listOf("встреча в 17:30", "meeting at 17:30", "17:30", "17:30 встреча")) {
            assertEquals(LocalDateTime(2024, 2, 28, 17, 30, 0), parse(text).single().targetDate, text)
        }
    }
    @Test fun explicitDateAndLeapDay() {
        for (text in listOf("встреча 29.02.2024", "29.02.2024 meeting")) {
            assertEquals(LocalDate(2024, 2, 29), parse(text).single().targetDate.date, text)
        }
    }
    @Test fun relativeHoursInBothLanguages() {
        for (text in listOf("купить через 5 часов", "buy in 5 hours")) {
            assertEquals(Instant.parse("2024-02-28T15:15:00Z"), parse(text).single().instant, text)
        }
    }
    @Test fun relativeDaysCrossLeapDayAndYearBoundary() {
        for (text in listOf("завтра", "tomorrow")) {
            assertEquals(LocalDate(2024, 2, 29), parse(text).single().targetDate.date, text)
            assertEquals(LocalDate(2025, 1, 1), parser.parse(text, Instant.parse("2024-12-31T10:15:00Z"), utc).single().targetDate.date)
        }
    }
    @Test fun absentEndAndRecurrenceRemainAbsent() {
        val match = parse("meeting at 17:30").single()
        assertNull(match.endDate)
        assertNull(match.endInstant)
        assertNull(match.recurrence)
        assertTrue(match.remainingText.contains("meeting", ignoreCase = true))
    }
    @Test fun monthlyAndYearlyRecurrenceKeepCalendarUnits() {
        for (text in listOf("каждый месяц", "every month")) assertEquals(1, parse(text).single().recurrence?.months, text)
        for (text in listOf("каждый год", "every year")) assertEquals(1, parse(text).single().recurrence?.years, text)
    }
    @Test fun emptyWhitespaceAndOrdinaryTextHaveNoDate() {
        for (text in listOf("", "  ", "\n\t", "buy milk", "купить молоко")) assertTrue(parse(text).isEmpty(), text)
    }
    @Test fun whitespaceDoesNotCreateEmptyTokens() {
        assertEquals(LocalDateTime(2024, 2, 28, 17, 30, 0), parse("  встреча\n\tв   17:30  ").single().targetDate)
    }
    @Test fun impossibleDatesAndOverflowingNumbersDoNotBecomeDates() {
        for (text in listOf("31.02.2024", "00.01.2024", "999999999999999999999 hours")) assertTrue(parse(text).isEmpty(), text)
    }
    @Test fun explicitTimezoneDoesNotLeakAcrossCalls() {
        val moscow = TimeZone.of("Europe/Moscow")
        assertEquals(Instant.parse("2024-02-28T14:30:00Z"), parser.parse("17:30", reference, moscow).single().instant)
        assertEquals(Instant.parse("2024-02-28T17:30:00Z"), parse("17:30").single().instant)
        assertEquals(Instant.parse("2024-02-28T14:30:00Z"), parser.parse("17:30", reference, moscow).single().instant)
    }
    @Test fun relativeHoursAreElapsedAcrossSpringDst() {
        val zone = TimeZone.of("America/New_York")
        val result = parser.parse("in 5 hours", Instant.parse("2024-03-10T05:30:00Z"), zone).single()
        assertEquals(Instant.parse("2024-03-10T10:30:00Z"), result.instant)
    }
    @Test fun relativeMonthClampsToLastDay() {
        val result = parser.parse("in 1 month", Instant.parse("2024-01-31T10:15:00Z"), utc).single()
        assertEquals(LocalDateTime(2024, 2, 29, 10, 15, 0), result.targetDate)
    }
    @Test fun recurrenceIntervalsAndExplicitEndDate() {
        for (text in listOf("every 2 days", "каждые 2 дня")) assertEquals(2, parse(text).singleOrNull()?.recurrence?.days, text)
        assertEquals(14, parse("every 2 weeks").singleOrNull()?.recurrence?.days)
        assertEquals(2, parse("every 2 months").singleOrNull()?.recurrence?.months)
        assertEquals(2, parse("every 2 years").singleOrNull()?.recurrence?.years)
        val range = parse("с 17:30 до 20:00").single()
        assertEquals(LocalDateTime(2024, 2, 28, 17, 30, 0), range.targetDate)
        assertEquals(LocalDateTime(2024, 2, 28, 20, 0, 0), range.endDate)
    }
    @Test fun injectedClockIsReadOnlyOncePerCall() {
        var reads = 0
        val clock = object : kotlin.time.Clock {
            override fun now(): Instant { reads++; return reference }
        }
        val match = DateParser(clock).parse("in 5 hours", timeZone = utc).single()
        assertEquals(Instant.parse("2024-02-28T15:15:00Z"), match.instant)
        assertEquals(1, reads)
    }
    @Test fun calendarDaysKeepWallTimeAcrossDst() {
        val zone = TimeZone.of("America/New_York")
        val result = parser.parse("in 1 day", Instant.parse("2024-03-09T17:00:00Z"), zone).single()
        assertEquals(Instant.parse("2024-03-10T16:00:00Z"), result.instant)
    }
    @Test fun thresholdOptionsRejectInvalidValues() {
        assertFailsWith<IllegalArgumentException> { ParseOptions(errorLimit = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { ParseOptions(minimumPrevalence = -1.0) }
    }
}

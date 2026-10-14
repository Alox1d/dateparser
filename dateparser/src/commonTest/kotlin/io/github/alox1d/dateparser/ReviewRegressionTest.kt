package io.github.alox1d.dateparser

import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.*

class ReviewRegressionTest {
    private val parser = DateParser()
    private val reference = Instant.parse("2024-02-28T10:15:00Z")
    private fun parse(text: String) = parser.parse(text, reference, TimeZone.UTC)

    @Test fun recurrenceAfterClockOrDateKeepsBothParts() {
        val clock = parse("17:30 every 2 days").single()
        assertEquals(LocalDateTime(2024, 2, 28, 17, 30), clock.targetDate)
        assertEquals(2, clock.recurrence?.days)
        val tomorrow = parse("tomorrow every 2 days").single()
        assertEquals(LocalDate(2024, 2, 29), tomorrow.targetDate.date)
        assertEquals(2, tomorrow.recurrence?.days)
    }
    @Test fun secondsUseOriginalClockToken() {
        assertEquals(LocalDateTime(2024, 2, 28, 17, 30, 59), parse("17:30:59").single().targetDate)
        assertTrue(parse("17:30:60").isEmpty())
    }
    @Test fun conjunctionsSupportMultipleResultsAndEmptyRemainder() {
        assertEquals(listOf(17, 20), parse("в 17:30 и 20:00").map { it.targetDate.hour })
        assertEquals(listOf(15, 12), parse("in 5 hours and in 2 hours").map { it.targetDate.hour })
        assertEquals(17, parse("17:30 and").single().targetDate.hour)
    }
    @Test fun dayPartWithoutHourDoesNotIndexPastCandidates() {
        assertEquals(LocalDateTime(2024, 2, 28, 10, 30), parse("30 минут вечером").single().targetDate)
        assertEquals(Instant.parse("2024-02-28T10:45:00Z"), parse("через 30 минут вечером").single().instant)
    }
    @Test fun explicitClockIsNotADurationInEitherOrder() {
        for (text in listOf("in 1 day at 17:30", "at 17:30 in 1 day", "через 1 день в 17:30")) {
            assertEquals(LocalDateTime(2024, 2, 29, 17, 30), parse(text).single().targetDate, text)
        }
        for (text in listOf("every month at 17:30", "17:30 every month")) {
            val match = parse(text).single()
            assertEquals(LocalDateTime(2024, 2, 28, 17, 30), match.targetDate, text)
            assertEquals(DateTimePeriod(months = 1), match.recurrence, text)
        }
    }
    @Test fun namedDatesValidateTheEffectiveYear() {
        val nonLeap = Instant.parse("2023-02-28T10:15:00Z")
        for (text in listOf("29 февраля", "29 February", "29 февраля 2023 года", "00 February")) {
            assertTrue(parser.parse(text, nonLeap, TimeZone.UTC).isEmpty(), text)
        }
        assertEquals(LocalDate(2024, 2, 29), parse("29 February").single().targetDate.date)
    }
    @Test fun halfHourUsesElapsedMinutesAcrossFallDst() {
        val result = parser.parse("через полчаса", Instant.parse("2024-11-03T05:45:00Z"), TimeZone.of("America/New_York")).single()
        assertEquals(Instant.parse("2024-11-03T06:15:00Z"), result.instant)
    }
    @Test fun compactMonthUnitsKeepCalendarMeaning() {
        for (text in listOf("in 1M", "через 1М")) assertEquals(LocalDate(2024, 3, 28), parse(text).single().targetDate.date, text)
        assertEquals(DateTimePeriod(months = 1), parse("every 1M").single().recurrence)
    }
    @Test fun inheritedHour24RolloverAlsoWorksWithRelativeDate() {
        assertEquals(LocalDateTime(2024, 2, 29, 0, 30), parse("24:30").single().targetDate)
        assertEquals(LocalDateTime(2024, 3, 1, 0, 30), parse("in 1 day at 24:30").single().targetDate)
    }
}

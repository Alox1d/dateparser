package io.github.alox1d.dateparser

import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.*

class AuditRegressionTest {
    private val parser = DateParser()
    private val reference = Instant.parse("2024-02-28T10:15:00Z")
    private fun parse(text: String) = parser.parse(text, reference, TimeZone.UTC)

    @Test fun tomorrowRangeEndInheritsStartCalendar() {
        for (text in listOf("tomorrow from 17:30 to 20:00", "завтра с 17:30 до 20:00")) {
            val match = parse(text).single()
            assertEquals(LocalDateTime(2024, 2, 29, 17, 30), match.targetDate, text)
            assertEquals(LocalDateTime(2024, 2, 29, 20, 0), match.endDate, text)
            assertEquals(Instant.parse("2024-02-29T20:00:00Z"), match.endInstant, text)
        }
    }
    @Test fun explicitAndRelativeCalendarRangesInheritStart() {
        val explicit = parse("29.02.2024 from 17:30 to 20:00").single()
        assertEquals(LocalDateTime(2024, 2, 29, 20, 0), explicit.endDate)
        val relative = parse("in 1 month from 17:30 until 20:00").single()
        assertEquals(LocalDateTime(2024, 3, 28, 20, 0), relative.endDate)
    }
    @Test fun implicitEarlierEndClockMovesToNextDay() {
        val match = parse("tomorrow from 23:00 to 01:00").single()
        assertEquals(LocalDateTime(2024, 3, 1, 1, 0), match.endDate)
    }
    @Test fun explicitEndDateIsPreservedAndInvertedRangeRejected() {
        assertEquals(LocalDate(2024, 3, 1), parse("from 29.02.2024 to 01.03.2024").single().endDate?.date)
        assertTrue(parse("from 29.02.2024 to 27.02.2024").isEmpty())
    }
    @Test fun dateOnlyContextPreservesAdjacentReminderText() {
        for (text in listOf("Buy milk, at 17:30", "at 17:30, Buy milk", "Buy milk, tomorrow", "tomorrow. Buy milk", "Buy milk. tomorrow")) {
            assertEquals("Buy milk", parse(text).single().remainingText, text)
        }
    }
    @Test fun punctuationAroundClocksDoesNotDiscardMatchesOrTitle() {
        for (clock in listOf("17:30!", "17:30?", "17:30;", "17:30,", "17:30.", "(17:30)", "17:30:59!")) {
            val match = parse("Buy Milk at $clock").single()
            assertEquals(17, match.targetDate.hour, clock)
            assertEquals(30, match.targetDate.minute, clock)
            assertEquals(if (clock.startsWith("17:30:59")) 59 else 0, match.targetDate.second, clock)
            assertEquals("Buy Milk", match.remainingText, clock)
        }
    }
    @Test fun punctuatedNumericDatesAndIndependentClocksRemainRecognized() {
        assertEquals(LocalDate(2024, 2, 29), parse("Meet (29.02.2024)!").single().targetDate.date)
        assertEquals(listOf(17, 20), parse("17:30! and 20:00?").map { it.targetDate.hour })
        assertEquals("Call Alice!", parse("Call Alice! at 17:30").single().remainingText)
        assertTrue(parse("Meeting17:30x").isEmpty())
    }
    @Test fun springGapRangeEndsAreNormalizedInBothZones() {
        for ((name, ref, end) in listOf(
            Triple("America/New_York", "2024-03-10T05:00:00Z", "2024-03-10T07:30:00Z"),
            Triple("Europe/Berlin", "2024-03-30T23:00:00Z", "2024-03-31T01:30:00Z")
        )) {
            val zone = TimeZone.of(name)
            val match = parser.parse("from 01:00 until 02:30", Instant.parse(ref), zone).single()
            assertEquals(Instant.parse(end), match.endInstant, name)
            assertEquals(LocalDateTime(2024, 3, if (name == "Europe/Berlin") 31 else 10, 3, 30), match.endDate, name)
        }
    }
    @Test fun fallOverlapRangeEndsKeepTheirResolvedWallTime() {
        for ((name, ref, end) in listOf(
            Triple("America/New_York", "2024-11-03T04:00:00Z", "2024-11-03T05:30:00Z"),
            Triple("Europe/Berlin", "2024-10-26T22:00:00Z", "2024-10-27T00:30:00Z")
        )) {
            val zone = TimeZone.of(name)
            val text = if (name == "America/New_York") "from 00:30 until 01:30" else "from 01:00 until 02:30"
            val match = parser.parse(text, Instant.parse(ref), zone).single()
            assertEquals(Instant.parse(end), match.endInstant, name)
            assertEquals(match.endInstant!!.toLocalDateTime(zone), match.endDate, name)
        }
    }
    @Test fun relativePeriodDoesNotOverwriteAnotherExplicitDate() {
        for (connector in listOf("then meet", "or", "meet", "and", ",")) {
            val matches = parse("call in 5 hours $connector 29.02.2024")
            assertEquals(listOf(Instant.parse("2024-02-28T15:15:00Z"), Instant.parse("2024-02-29T10:15:00Z")), matches.map { it.instant }, connector)
        }
    }
    @Test fun halfHourPeriodDoesNotOverwriteAnotherExplicitDate() {
        assertEquals(listOf(Instant.parse("2024-02-28T10:45:00Z"), Instant.parse("2024-02-29T10:15:00Z")),
            parse("через полчаса затем 29.02.2024").map { it.instant })
    }
    @Test fun tomorrowRangesUseStartCalendarAcrossDst() {
        for ((name, ref, end) in listOf(
            Triple("America/New_York", "2024-03-09T15:15:00Z", "2024-03-11T00:00:00Z"),
            Triple("Europe/Berlin", "2024-03-30T09:15:00Z", "2024-03-31T18:00:00Z")
        )) {
            val zone = TimeZone.of(name)
            val match = parser.parse("tomorrow from 17:30 until 20:00", Instant.parse(ref), zone).single()
            assertEquals(Instant.parse(end), match.endInstant, name)
            assertEquals(match.instant.toLocalDateTime(zone), match.targetDate, name)
        }
    }
    @Test fun dateOnlyPhrasesStillKeepReferenceClock() {
        assertEquals(LocalDateTime(2024, 2, 29, 10, 15), parse("tomorrow").single().targetDate)
    }
    @Test fun relativeRangeEndIsRejectedInsteadOfChangingStart() {
        for (text in listOf("from 29.02.2024 until in 2 days", "from 17:30 until in 2 hours", "с 29.02.2024 до через 2 дня")) {
            assertTrue(parse(text).isEmpty(), text)
        }
    }
    @Test fun punctuatedRussianCountsKeepDurationAndRecurrenceMeaning() {
        assertEquals(LocalDateTime(2024, 3, 1, 10, 15), parse("через (2) дня").single().targetDate)
        assertEquals(DateTimePeriod(days = 2), parse("каждые (2) дня").single().recurrence)
    }
    @Test fun invalidRecurrenceCannotLeaveAnUnrelatedClockMatch() {
        for (text in listOf("every -100 days at 17:30", "каждые -100 дней в 17:30", "every +0 days at 17:30", "every 000 days at 17:30")) {
            assertTrue(parse(text).isEmpty(), text)
        }
    }
    @Test fun longNumericPeriodsAreValidOrRejectedWithoutCrashing() {
        assertEquals(DateTimePeriod(days = 100), parse("every 100 days").single().recurrence)
        assertTrue(parse("in 999999999 years").isEmpty())
        assertTrue(parse("in 2147483647 weeks").isEmpty())
        assertTrue(parse("in 9999999999999999999 years").isEmpty())
    }
    @Test fun fourDigitPeriodCountsAreNotMistakenForYears() {
        val match = parse("every 1000 days at 5PM").single()
        assertEquals(DateTimePeriod(days = 1000), match.recurrence)
        assertEquals(LocalDateTime(2024, 2, 28, 17, 0), match.targetDate)
        assertEquals(DateTimePeriod(years = 1000), parse("every 1000 years").single().recurrence)
        assertEquals(reference.plus(DateTimePeriod(days = 1000), TimeZone.UTC), parse("in 1000 days").single().instant)
        assertEquals(2026, parse("2026 year").single().targetDate.year)
    }
    @Test fun nonPositiveRecurrenceDoesNotBecomeAReminder() {
        for (text in listOf("every 0 days", "every 0 years", "every 0 seconds", "every -1 days", "каждые 0 дней", "every -2 hours", "every -100 days", "every -999999 seconds")) {
            assertTrue(parse(text).isEmpty(), text)
        }
    }
}

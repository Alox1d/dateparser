package io.github.alox1d.dateparser

import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.*

class EnglishApiTest {
    private val parser = DateParser()
    private val reference = Instant.parse("2024-02-28T10:15:29Z")
    private fun parse(text: String) = parser.parse(text, reference, TimeZone.UTC)

    @Test fun publicApiRecognizesUsAndEuropeanEnglishDates() {
        for (text in listOf("Email Alice on December 31, 2026 at 5PM", "Email Alice on 31st Dec 2026 at 5 pm")) {
            val match = parse(text).single()
            assertEquals(LocalDateTime(2026, 12, 31, 17, 0, 0), match.targetDate, text)
            assertEquals("Email Alice", match.remainingText, text)
        }
    }
    @Test fun todayAndYesterdayKeepReferenceClock() {
        assertEquals(LocalDateTime(2024, 2, 28, 10, 15, 29), parse("today").single().targetDate)
        assertEquals(LocalDateTime(2024, 2, 27, 10, 15, 29), parse("yesterday").single().targetDate)
    }
    @Test fun numericDateOrderIsExplicitAndExistingDotOrderUnchanged() {
        assertEquals(LocalDate(2026, 3, 4), parser.parse("03/04/2026", DateOrder.MDY, reference, TimeZone.UTC).single().targetDate.date)
        assertEquals(LocalDate(2026, 4, 3), parser.parse("03/04/2026", DateOrder.DMY, reference, TimeZone.UTC).single().targetDate.date)
        assertEquals(LocalDate(2026, 12, 31), parser.parse("2026/12/31", DateOrder.YMD, reference, TimeZone.UTC).single().targetDate.date)
        assertEquals(LocalDate(2026, 4, 3), parser.parse("03.04.2026", DateOrder.MDY, reference, TimeZone.UTC).single().targetDate.date)
        assertEquals(LocalDate(2026, 12, 31), parse("2026-12-31").single().targetDate.date)
    }
    @Test fun publicApiUsesActualYearForLeapDayAndClockValidation() {
        val nonLeap = Instant.parse("2025-02-28T10:15:29Z")
        assertTrue(parser.parse("Meet February 29 at 5PM", nonLeap, TimeZone.UTC).isEmpty())
        assertEquals(LocalDateTime(2024, 2, 29, 17, 0), parse("Meet February 29 at 5PM").single().targetDate)
    }
    @Test fun usAmPmRangeHasChronologicalBounds() {
        val match = parse("tomorrow from 5PM to 8PM").single()
        assertEquals(LocalDateTime(2024, 2, 29, 17, 0), match.targetDate)
        assertEquals(LocalDateTime(2024, 2, 29, 20, 0), match.endDate)
        assertTrue(match.endInstant!! >= match.instant)
    }
    @Test fun invalidOrAmbiguousDateCannotYieldPartialTomorrowMatch() {
        for (text in listOf("tomorrow on 03/04/2026", "tomorrow at 13pm", "Meet February 30, 2026 at 5pm")) {
            assertTrue(parse(text).isEmpty(), text)
        }
    }
}

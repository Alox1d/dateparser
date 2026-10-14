package io.github.alox1d.dateparser

import io.github.alox1d.dateparser.internal.EnglishGrammar
import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.*

class EnglishGrammarTest {
    private val reference = Instant.parse("2024-02-28T10:15:21Z")
    private fun parse(text: String, order: DateOrder? = null) =
        DateParser().parse(EnglishGrammar.normalize(text, order, reference.toLocalDateTime(TimeZone.UTC).year), reference, TimeZone.UTC)

    @Test fun namedDatesUseNaturalYearAndBothOrders() {
        for (text in listOf("31 December 2026", "December 31, 2026", "Dec. 31st,2026", "31st Dec 2026", "December 31,2026")) {
            assertEquals(LocalDateTime(2026,12,31,10,15,21), parse(text).single().targetDate, text)
        }
        assertEquals(LocalDate(2024,2,29), parse("February 29").single().targetDate.date)
        assertEquals(LocalDate(2024,2,29), parse("29th Feb.").single().targetDate.date)
    }
    @Test fun attachedAndSpacedAmPmZeroMissingFields() {
        for (text in listOf("5pm", "5 PM", "5p.m.", "5 p.m.")) {
            assertEquals(LocalDateTime(2024,2,28,17,0,0), parse(text).single().targetDate, text)
        }
        assertEquals(LocalDateTime(2024,2,28,17,30,0), parse("5:30PM").single().targetDate)
        assertEquals(LocalDateTime(2024,2,28,0,0,0), parse("12AM").single().targetDate)
        assertEquals(LocalDateTime(2024,2,28,12,0,0), parse("12PM").single().targetDate)
        assertEquals(LocalDateTime(2024,2,28,23,59,59), parse("11:59:59pm").single().targetDate)
        assertEquals(LocalDateTime(2024,2,29,17,0,0), parse("tomorrow at 5pm").single().targetDate)
    }
    @Test fun numericDatesResolveOnlyKnownOrder() {
        for (text in listOf("13/04/2026", "04/13/2026", "2026-04-13")) {
            assertEquals(LocalDate(2026,4,13), parse(text).single().targetDate.date, text)
        }
        assertTrue(parse("03/04/2026").isEmpty())
        assertEquals(LocalDate(2026,4,3), parse("03/04/2026",DateOrder.DMY).single().targetDate.date)
        assertEquals(LocalDate(2026,3,4), parse("03/04/2026",DateOrder.MDY).single().targetDate.date)
        assertEquals(LocalDate(2026,4,3), parse("2026/04/03",DateOrder.YMD).single().targetDate.date)
        assertEquals(LocalDate(2024,4,13), parse("13/04").single().targetDate.date)
        assertTrue(parse("03/04").isEmpty())
        assertEquals(LocalDate(2026,4,3), parse("03.04.2026",DateOrder.MDY).single().targetDate.date)
    }
    @Test fun invalidDateAndClockTokensNeverBecomePartialMatches() {
        for (text in listOf("February 30, 2026", "29 February 2025", "31 April", "February 29, 0000", "13pm", "0AM", "5:60PM", "12:10:60am", "2026-02-30", "31/04/2026", "03/04/2026 at 5pm", "tomorrow at 13pm", "tomorrow at 1:2:3:4pm", "March 12, 10000", "tomorrow at 999999999999999999999pm", "01/02/03/04", "2026-02-29")) {
            assertTrue(parse(text).isEmpty(), text)
        }
    }
    @Test fun todayAndYesterdayRetainLocalClockAcrossBoundaries() {
        assertEquals(LocalDateTime(2024,2,28,10,15,21),parse("today").single().targetDate)
        assertEquals(LocalDateTime(2024,2,27,10,15,21),parse("yesterday").single().targetDate)
        val yearStart=Instant.parse("2025-01-01T10:15:21Z")
        val monthStart=Instant.parse("2024-03-01T10:15:21Z")
        assertEquals(LocalDate(2024,12,31),DateParser().parse(EnglishGrammar.normalize("yesterday",null),yearStart,TimeZone.UTC).single().targetDate.date)
        assertEquals(LocalDate(2024,2,29),DateParser().parse(EnglishGrammar.normalize("yesterday",null),monthStart,TimeZone.UTC).single().targetDate.date)
    }
    @Test fun nextPeriodsAndBareWeekdays() {
        assertEquals(LocalDate(2024,3,6), parse("next week").single().targetDate.date)
        assertEquals(LocalDate(2024,3,28), parse("next month").single().targetDate.date)
        assertEquals(LocalDate(2025,2,28), parse("next year").single().targetDate.date)
        assertEquals(LocalDate(2024,3,1), parse("Friday").single().targetDate.date)
    }
    @Test fun compoundPeriodsRemainOneExpression() {
        assertEquals(LocalDate(2024,3,30), parse("in 1 month and 2 days").single().targetDate.date)
        val recurrence=parse("every 1 month and 2 days").single().recurrence
        assertEquals(1,recurrence?.months)
        assertEquals(2,recurrence?.days)
        assertEquals(2,parse("in 5 hours and in 2 hours").size)
    }
    @Test fun namedDateAbbreviationsAndOrdinalsValidateBoundaries() {
        for ((text, month) in listOf("Jan 1st 2026" to 1, "Feb 2nd 2026" to 2, "Mar 3rd 2026" to 3, "Apr 4th 2026" to 4, "Sep 11th 2026" to 9, "Sept. 12th 2026" to 9, "Oct 13th 2026" to 10)) {
            assertEquals(month,parse(text).single().targetDate.month.number,text)
        }
        for (text in listOf("February 21th 2026", "21rd February 2026", "January 100 2026")) assertTrue(parse(text).isEmpty(),text)
        assertEquals(LocalDate(2024,2,29),parse("February 29, 2024").single().targetDate.date)
        assertEquals(LocalDate(2026,12,31),parse("December 31, 2026 at 11:59PM").single().targetDate.date)
    }
    @Test fun weekdayNamedDateIsOneCoherentCalendarExpression() {
        for (text in listOf("Friday March 13 2026", "Friday, 13 March 2026", "Friday,13March2026", "on Friday March 13 2026 at 5pm")) {
            assertEquals(LocalDate(2026,3,13),parse(text).single().targetDate.date,text)
        }
        for (text in listOf("Thursday March 13 2026", "Monday, 13 March 2026 at 5pm")) assertTrue(parse(text).isEmpty(),text)
    }
    @Test fun malformedNamedDatesCannotLeaveAClockMatch() {
        for (text in listOf("March 1000 2026 at 5pm", "March1000 2026 at5pm", "-12 March 2026", "-12March2026 at 5pm", "+12 March 2026 at 5pm", "1000 March 2026 at 5pm")) assertTrue(parse(text).isEmpty(),text)
        val nonLeap=Instant.parse("2025-02-28T10:15:21Z")
        for (text in listOf("February 29 at 5pm", "29 Feb at 5pm")) {
            assertTrue(DateParser().parse(EnglishGrammar.normalize(text,null,2025),nonLeap,TimeZone.UTC).isEmpty(),text)
        }
    }
    @Test fun nonpositiveCompoundComponentsRejectWholeExpression() {
        for (text in listOf("every -1 month and 2 days", "every 1 month and -2 days", "in 0 months and 2 days", "every 0 months and 2 days", "in 1 month and 0 days at 5pm")) assertTrue(parse(text).isEmpty(),text)
        assertEquals(LocalDate(2024,3,30),parse("in 1 month and 2 days").single().targetDate.date)
        assertEquals(2,parse("in 5 hours and in 2 hours").size)
    }
    @Test fun invalidDottedDateCannotLeaveAClockMatch() {
        for (text in listOf("31.02.2026 at 5pm", "29.02.2025 at 5pm", "00.01.2026 at 5pm")) assertTrue(parse(text).isEmpty(),text)
        assertEquals(LocalDate(2024,2,29),parse("29.02.2024 at 5pm").single().targetDate.date)
        assertEquals(17,parse("17.30").single().targetDate.hour)
        assertEquals(1,parse("1.30").single().targetDate.hour)
        assertEquals(LocalDateTime(2024,3,1,17,30,21),parse("Friday at 17.30").single().targetDate)
        assertEquals(LocalDateTime(2024,3,1,17,30,21),parse("Friday 17.30").single().targetDate)
    }
    @Test fun versionAndIpTokensStayInReminderText() {
        for ((text, remainder) in listOf("Update version 1.2.0 tomorrow" to "Update version 1.2.0", "Check 192.168.0.1 tomorrow" to "Check 192.168.0.1", "Check 127.0.0.1 tomorrow" to "Check 127.0.0.1", "Check 1.02.03.4 tomorrow" to "Check 1.02.03.4")) {
            val result=parse(text).single()
            assertEquals(LocalDate(2024,2,29),result.targetDate.date,text)
            assertEquals(remainder,result.remainingText,text)
        }
        assertTrue(parse("31.02.2026 at 5pm").isEmpty())
    }
    @Test fun missingAmPmClockFieldsRejectWholeInput() {
        for (text in listOf("tomorrow at 5:pm", "tomorrow at 5:30:pm", "5:pm", "5:30:pm")) assertTrue(parse(text).isEmpty(),text)
        assertEquals(LocalDateTime(2024,2,29,17,30,0),parse("tomorrow at 5:30pm").single().targetDate)
    }
    @Test fun temporalRewritesPreserveTitleCaseAndIndependentConjunction() {
        val result=parse("Email Alice and Bob on December 31,2026 at 5PM!").single()
        assertEquals(LocalDateTime(2026,12,31,17,0,0),result.targetDate)
        assertEquals("Email Alice and Bob !",result.remainingText)
    }
}

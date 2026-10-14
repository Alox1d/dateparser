package io.github.alox1d.dateparser.sample

import io.github.alox1d.dateparser.DateOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class ReminderDraftTest {
    private val reference = Instant.parse("2026-07-03T10:15:00Z")

    @Test fun keepsTheReminderTitleAndBothRangeBoundaries() {
        val draft = assertNotNull(parseReminderDraft(
            "Buy milk, tomorrow from 5PM to 8PM", reference, TimeZone.UTC
        ))
        assertEquals("Buy milk", draft.title)
        assertEquals(Instant.parse("2026-07-04T17:00:00Z"), draft.startsAt)
        assertEquals(Instant.parse("2026-07-04T20:00:00Z"), draft.endsAt)
    }

    @Test fun appliesTheCallersUsOrEuropeanNumericOrder() {
        val us = assertNotNull(parseReminderDraft("03/04/2026", reference, TimeZone.UTC, DateOrder.MDY))
        val europe = assertNotNull(parseReminderDraft("03/04/2026", reference, TimeZone.UTC, DateOrder.DMY))
        assertEquals(Instant.parse("2026-03-04T10:15:00Z"), us.startsAt)
        assertEquals(Instant.parse("2026-04-03T10:15:00Z"), europe.startsAt)
        assertNull(us.endsAt)
    }

    @Test fun resolvesNamedDatesInTheUsersTimezone() {
        val berlin = TimeZone.of("Europe/Berlin")
        val draft = assertNotNull(parseReminderDraft("Meet on July 4, 2026 at 5PM", reference, berlin))
        assertEquals(Instant.parse("2026-07-04T15:00:00Z"), draft.startsAt)
        assertEquals(berlin, draft.timeZone)
        assertEquals("Meet", draft.title)
    }

    @Test fun requestsManualReviewForUnsupportedOrAmbiguousInput() {
        for (text in listOf("", "Buy milk", "03/04/2026", "July 32, 2026")) {
            assertNull(parseReminderDraft(text, reference, TimeZone.UTC), text)
        }
    }

    @Test fun doesNotSilentlyTurnRecurrenceIntoASingleReminder() {
        assertNull(parseReminderDraft("every 2 days", reference, TimeZone.UTC))
    }
}

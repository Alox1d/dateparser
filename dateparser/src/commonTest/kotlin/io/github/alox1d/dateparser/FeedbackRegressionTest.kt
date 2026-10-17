package io.github.alox1d.dateparser

import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class FeedbackRegressionTest {
    private val parser = DateParser()
    private val reference = Instant.parse("2026-10-15T10:15:00Z")

    @Test fun unconsumedBareClockDoesNotSilentlyScheduleReferenceHour() {
        for (input in listOf("Remind me tomorow at 7", "Buy milk tomorrow at 7", "🛒 Buy milk tomorrow at 7 🥛")) {
            assertTrue(parser.parse(input, reference, TimeZone.UTC).isEmpty(), input)
        }
    }

    @Test fun explicitAmPmAndTwentyFourHourClocksRemainSupported() {
        for (input in listOf("Buy milk tomorrow 7pm", "Buy milk tomorrow at 7pm", "Buy milk tomorrow at 19:00")) {
            assertEquals(Instant.parse("2026-10-16T19:00:00Z"), parser.parse(input, reference, TimeZone.UTC).single().instant, input)
        }
    }

    @Test fun invalidLeapDayCannotBecomeValidPartialMatch() {
        assertTrue(parser.parse("Pay on February 30, 2026 at 5pm", reference, TimeZone.UTC).isEmpty())
    }
}

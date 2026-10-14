import io.github.alox1d.dateparser.DateParser
import io.github.alox1d.dateparser.DateOrder
import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.*

class PublishedParserTest {
    private val parser = DateParser()
    private val reference = Instant.parse("2024-02-28T10:15:00Z")
    private fun parse(text: String) = parser.parse(text, reference, TimeZone.UTC)

    @Test fun parsesFromPublishedCommonMetadata() {
        for (text in listOf("купить через 5 часов", "buy in 5 hours")) {
            assertEquals(Instant.parse("2024-02-28T15:15:00Z"), parse(text).single().instant)
        }
    }
    @Test fun englishCalendarAndClockForms() {
        for (text in listOf("Meet on December 31, 2026 at 5PM", "Meet on 31st Dec 2026 at 5 pm")) {
            val match = parse(text).single()
            assertEquals(LocalDateTime(2026, 12, 31, 17, 0), match.targetDate)
            assertEquals("Meet", match.remainingText)
        }
    }
    @Test fun explicitUsAndEuropeanNumericOrder() {
        assertTrue(parse("03/04/2026").isEmpty())
        assertEquals(LocalDate(2026, 3, 4), parser.parse("03/04/2026", DateOrder.MDY, reference, TimeZone.UTC).single().targetDate.date)
        assertEquals(LocalDate(2026, 4, 3), parser.parse("03/04/2026", DateOrder.DMY, reference, TimeZone.UTC).single().targetDate.date)
    }
    @Test fun tomorrowRangeAndReminder() {
        val match = parse("Buy milk, tomorrow from 17:30 to 20:00").single()
        assertEquals(LocalDateTime(2024, 2, 29, 17, 30), match.targetDate)
        assertEquals(LocalDateTime(2024, 2, 29, 20, 0), match.endDate)
        assertEquals("Buy milk", match.remainingText)
    }
    @Test fun calendarCompoundRecurrence() {
        assertEquals(DateTimePeriod(months = 1, days = 2), parse("every 1 month and 2 days").single().recurrence)
        assertTrue(parse("every 0 days").isEmpty())
    }
    @Test fun dstEndRoundTrip() {
        val zone = TimeZone.of("America/New_York")
        val match = parser.parse("from 01:00 until 02:30", Instant.parse("2024-03-10T05:00:00Z"), zone).single()
        assertEquals(LocalDateTime(2024, 3, 10, 3, 30), match.endDate)
        assertEquals(match.endDate, match.endInstant?.toLocalDateTime(zone))
    }
}

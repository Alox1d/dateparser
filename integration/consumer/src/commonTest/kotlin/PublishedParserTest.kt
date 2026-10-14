import io.github.alox1d.dateparser.DateParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class PublishedParserTest {
    @Test fun parsesFromPublishedCommonMetadata() {
        val reference = Instant.parse("2024-02-28T10:15:00Z")
        for (text in listOf("купить через 5 часов", "buy in 5 hours")) {
            assertEquals(Instant.parse("2024-02-28T15:15:00Z"), DateParser().parse(text, reference, TimeZone.UTC).single().instant)
        }
    }
}

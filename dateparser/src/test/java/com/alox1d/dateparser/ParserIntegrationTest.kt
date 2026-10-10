package com.alox1d.dateparser

import com.huawei.wordnote.domain.services.dateparser.ParsedDate
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ParserIntegrationTest {
    private lateinit var originalZone: TimeZone

    @Before fun prepare() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        ParsedDate.prepareParse(RuntimeEnvironment.getApplication())
    }
    @After fun restore() { TimeZone.setDefault(originalZone) }

    @Test fun `extracts Russian clock time using packaged rules`() {
        val result = ParsedDate.parseDate("встреча в 17:30").single().valueOf().target_date
        assertEquals(17, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, result.get(Calendar.MINUTE))
    }

    @Test fun `extracts explicit date from text`() {
        val result = ParsedDate.parseDate("встреча 03.01.2024").single().valueOf().target_date
        assertEquals(2024, result.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, result.get(Calendar.MONTH))
        assertEquals(3, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test fun `relative hours work in Russian and English`() {
        for (text in listOf("купить через 5 часов", "buy in 5 hours")) {
            val before = System.currentTimeMillis()
            val result = ParsedDate.parseDate(text).single().valueOf().target_date.timeInMillis
            val after = System.currentTimeMillis()
            val offset = TimeUnit.HOURS.toMillis(5)
            assertTrue("$text: $result", result >= before + offset - 1000 && result <= after + offset)
        }
    }

    @Test fun `empty input produces no dates`() {
        assertTrue(ParsedDate.parseDate("").isEmpty())
    }
    @Test fun `clock time can be the first token`() {
        for (text in listOf("17:30", "17:30 встреча")) {
            val result = ParsedDate.parseDate(text).single().valueOf().target_date
            assertEquals(17, result.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, result.get(Calendar.MINUTE))
        }
    }

    @Test fun `date can be the first token`() {
        val result = ParsedDate.parseDate("03.01.2024 встреча").single().valueOf().target_date
        assertEquals(2024, result.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, result.get(Calendar.MONTH))
        assertEquals(3, result.get(Calendar.DAY_OF_MONTH))
    }
}

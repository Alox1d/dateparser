@file:Suppress("DEPRECATION")
package io.github.alox1d.dateparser

import com.huawei.wordnote.domain.services.dateparser.ParsedDate
import java.util.Calendar
import kotlin.test.*

class LegacyAdapterTest {
    @Test fun legacyFacadeNeedsNoContextAndRetainsCalendar() {
        ParsedDate.prepareParse(null)
        val parsed = ParsedDate.parseDate("встреча 29.02.2024 в 17:30").single()
        val value = parsed.valueOf()
        assertEquals(2024, value.target_date.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, value.target_date.get(Calendar.MONTH))
        assertEquals(29, value.target_date.get(Calendar.DAY_OF_MONTH))
        assertEquals(17, value.target_date.get(Calendar.HOUR_OF_DAY))
        assertEquals(0L, value.max_date)
        assertEquals(0L, value.period_time)
    }
    @Test fun legacyCalendarDoesNotMutateStoredResult() {
        val parsed = ParsedDate.parseDate("29.02.2024").single()
        parsed.valueOf().target_date.set(Calendar.YEAR, 2030)
        assertEquals(2024, parsed.valueOf().target_date.get(Calendar.YEAR))
    }
    @Test fun legacyMonthlyDurationIsExplicitCompatibilityConvention() {
        assertEquals(31L * 86400000L, ParsedDate.parseDate("every month").single().valueOf().period_time)
    }
}

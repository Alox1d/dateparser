package com.alox1d.dateparser

import com.huawei.wordnote.domain.services.dateparser.ParsedDate
import com.huawei.wordnote.domain.services.dateparser.TimeSet
import org.junit.Assert.*
import org.junit.Test
import ru.alox1d.wordnote.huawei.domain.services.dateparser.*

class ParsedTimeDistanceTest {
    private fun time(vararg indexes: Int, type: DateTypes = DateTypes.target_date) =
        ParsedTime(type, TimeTypes.hours, 17, ArrayList(indexes.toList()), 0, 100)

    @Test fun `measures gap after and before an existing expression`() {
        val set = TimeSet(0).apply { target_date.hours = time(3, 4) }
        assertEquals(4, ParsedDate.distanceBetweenParsedTimeAndTimeList(time(8), set))
        assertEquals(3, ParsedDate.distanceBetweenParsedTimeAndTimeList(time(0), set))
    }

    @Test fun `overlapping expressions have no gap`() {
        val set = TimeSet(0).apply { target_date.hours = time(3, 4) }
        assertEquals(0, ParsedDate.distanceBetweenParsedTimeAndTimeList(time(4, 5), set))
    }

    @Test fun `empty lists are safe`() {
        assertEquals(0, ParsedDate.distanceBetweenParsedTimeAndTimeList(time(8), TimeSet(0)))
        val set = TimeSet(0).apply { target_date.hours = time(3) }
        assertEquals(0, ParsedDate.distanceBetweenParsedTimeAndTimeList(time(), set))
    }

    @Test fun `distance does not duplicate source indexes`() {
        val stored = time(3, 4)
        val incoming = time(8)
        val set = TimeSet(0).apply { target_date.hours = stored }
        repeat(3) { ParsedDate.distanceBetweenParsedTimeAndTimeList(incoming, set) }
        assertEquals(listOf(3, 4), stored.indexes)
        assertEquals(listOf(8), incoming.indexes)
    }

    @Test fun `recurrence and maximum expressions use their own lists`() {
        val set = TimeSet(0).apply {
            target_date.hours = time(1)
            period_time.hours = time(5, type = DateTypes.period_time)
            max_date.hours = time(10, type = DateTypes.max_date)
        }
        assertEquals(2, ParsedDate.distanceBetweenParsedTimeAndTimeList(time(7, type = DateTypes.period_time), set))
        assertEquals(3, ParsedDate.distanceBetweenParsedTimeAndTimeList(time(13, type = DateTypes.max_date), set))
    }
}

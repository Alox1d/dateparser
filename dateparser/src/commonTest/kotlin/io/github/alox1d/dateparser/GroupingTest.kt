package io.github.alox1d.dateparser

import io.github.alox1d.dateparser.internal.*
import kotlin.test.*
import kotlinx.datetime.LocalDateTime

class GroupingTest {
    private val engine = ParseEngine(LocalDateTime(2024, 2, 28, 10, 15))
    private fun time(vararg indexes: Int, type: DateTypes = DateTypes.target_date) =
        ParsedTime(type, TimeTypes.hours, 17, ArrayList(indexes.toList()), 0, 100)
    @Test fun gapIsSymmetricBeforeAndAfter() {
        val set = TimeSet(0).apply { target_date.hours = time(3, 4) }
        assertEquals(4, engine.distanceBetweenParsedTimeAndTimeList(time(8), set))
        assertEquals(3, engine.distanceBetweenParsedTimeAndTimeList(time(0), set))
    }
    @Test fun overlapAndEmptyAreSafe() {
        val set = TimeSet(0).apply { target_date.hours = time(3, 4) }
        assertEquals(0, engine.distanceBetweenParsedTimeAndTimeList(time(4, 5), set))
        assertEquals(0, engine.distanceBetweenParsedTimeAndTimeList(time(8), TimeSet(0)))
        assertEquals(0, engine.distanceBetweenParsedTimeAndTimeList(time(), set))
    }
    @Test fun groupingDoesNotMutateIndexes() {
        val stored = time(3, 4)
        val incoming = time(8)
        val set = TimeSet(0).apply { target_date.hours = stored }
        repeat(3) { engine.distanceBetweenParsedTimeAndTimeList(incoming, set) }
        assertEquals(listOf(3, 4), stored.indexes)
        assertEquals(listOf(8), incoming.indexes)
    }
    @Test fun recurrenceAndEndUseTheirOwnLists() {
        val set = TimeSet(0).apply {
            target_date.hours = time(1)
            period_time.hours = time(5, type = DateTypes.period_time)
            max_date.hours = time(10, type = DateTypes.max_date)
        }
        assertEquals(2, engine.distanceBetweenParsedTimeAndTimeList(time(7, type = DateTypes.period_time), set))
        assertEquals(3, engine.distanceBetweenParsedTimeAndTimeList(time(13, type = DateTypes.max_date), set))
    }
}

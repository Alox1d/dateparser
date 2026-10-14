package com.huawei.wordnote.domain.services.dateparser

import android.content.Context
import io.github.alox1d.dateparser.DateMatch
import io.github.alox1d.dateparser.DateParser
import io.github.alox1d.dateparser.ParseOptions
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Instant
import kotlinx.datetime.plus

/** Compatibility facade for the historical Android entry points. Prefer the common DateParser API. */
@Deprecated("Use io.github.alox1d.dateparser.DateParser")
public class ParsedDate private constructor(private val match: DateMatch) {
    public val string: String get() = match.remainingText
    public val confidence: Int get() = match.confidence
    override fun toString(): String = string

    public class DateParserModel(
        public val target_date: Calendar,
        public val period_time: Long,
        /** Zero means no end date. */
        public val max_date: Long
    )

    public fun valueOf(): DateParserModel {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(match.timeZone.id)).apply {
            timeInMillis = match.instant.toEpochMilliseconds()
        }
        // Preserve the old UTC-epoch duration convention only in this deprecated facade.
        val period = match.recurrence?.let {
            Instant.fromEpochMilliseconds(0).plus(it, kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()
        } ?: 0L
        return DateParserModel(calendar, period, match.endInstant?.toEpochMilliseconds() ?: 0L)
    }

    public companion object {
        /** Retained for source compatibility. Context is neither needed nor retained. */
        @Suppress("UNUSED_PARAMETER")
        public fun prepareParse(context: Context?) { }
        public fun parseDate(string: String, errorLimit: Double = 1.0, minimumPrevalence: Double = 50.0): ArrayList<ParsedDate> =
            ArrayList(DateParser().parse(string, options = ParseOptions(errorLimit, minimumPrevalence)).map { ParsedDate(it) })
    }
}

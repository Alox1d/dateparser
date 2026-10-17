package io.github.alox1d.dateparser.sample

import io.github.alox1d.dateparser.DateOrder
import io.github.alox1d.dateparser.DateParser
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

data class ReminderDraft(
    val title: String,
    val startsAt: Instant,
    val endsAt: Instant?,
    val timeZone: TimeZone
)

fun parseReminderDraft(
    text: String,
    reference: Instant,
    timeZone: TimeZone,
    dateOrder: DateOrder? = null
): ReminderDraft? {
    val parser = DateParser()
    val matches = if (dateOrder == null) {
        parser.parse(text, reference, timeZone)
    } else {
        parser.parse(text, dateOrder, reference, timeZone)
    }
    val match = matches.singleOrNull() ?: return null
    // A recurring interval needs its own scheduling UI and policy.
    if (match.recurrence != null) return null
    // The parser preserves reminder text; the sample trims separator punctuation for UI titles.
    val title = match.remainingText.trim().trim(',', '-', '—', ' ')
    return ReminderDraft(title, match.instant, match.endInstant, match.timeZone)
}

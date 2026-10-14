package io.github.alox1d.dateparser

import io.github.alox1d.dateparser.internal.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.*

/** Rule matching thresholds. Confidence is an accumulated rule score, not a probability. */
public data class ParseOptions(public val errorLimit: Double = 1.0, public val minimumPrevalence: Double = 50.0) {
    init {
        require(errorLimit.isFinite() && errorLimit in 0.0..1.0) { "errorLimit must be in 0..1" }
        require(minimumPrevalence.isFinite() && minimumPrevalence in 0.0..100.0) { "minimumPrevalence must be in 0..100" }
    }
}

/** An immutable resolved match. Missing end/recurrence are null; months and years retain calendar meaning. */
public data class DateMatch(
    public val targetDate: LocalDateTime,
    public val instant: Instant,
    public val timeZone: TimeZone,
    public val recurrence: DateTimePeriod?,
    public val endDate: LocalDateTime?,
    public val endInstant: Instant?,
    public val remainingText: String,
    public val confidence: Int
)

/** Offline Russian/English parser. All working state is local to a parse call. */
public class DateParser(private val clock: Clock = Clock.System) {
    public fun parse(
        text: String,
        reference: Instant = clock.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        options: ParseOptions = ParseOptions()
    ): List<DateMatch> {
        if (text.isBlank()) return emptyList()
        val localReference = reference.toLocalDateTime(timeZone)
        val fields = try {
            ParseEngine(localReference).parseDate(text, options.errorLimit, options.minimumPrevalence)
        } catch (_: NumberFormatException) {
            return emptyList()
        }
        return fields.mapNotNull { parsed ->
            if (!validExplicitDate(parsed.target_date, localReference) || !validExplicitDate(parsed.max_date, localReference)) return@mapNotNull null
            val instant = parsed.relativePeriod?.let { period ->
                val shifted = reference.plus(period, timeZone)
                val local = shifted.toLocalDateTime(timeZone)
                val fields = parsed.target_date
                if (fields.fixedHours != null || fields.fixedMinutes != null || fields.fixedSeconds != null) {
                    val clockFields = TimeList().apply {
                        hours = fields.fixedHours; minutes = fields.fixedMinutes; seconds = fields.fixedSeconds
                    }
                    resolve(clockFields, local).toInstant(timeZone)
                } else shifted
            } ?: resolve(parsed.target_date, localReference).toInstant(timeZone)
            val end = parsed.max_date.takeIf { it.hasFields() }?.let { resolve(it, localReference) }
            DateMatch(
                instant.toLocalDateTime(timeZone), instant, timeZone,
                parsed.period_time.takeIf { it.hasFields() }?.toPeriod(),
                end, end?.toInstant(timeZone), parsed.toString(), parsed.confidence
            )
        }
    }
}

private fun TimeList.hasFields(): Boolean = listOf(years, months, dates, hours, minutes, seconds).any { it != null }
private fun TimeList.toPeriod(): DateTimePeriod = DateTimePeriod(
    years = years as Int? ?: 0, months = months as Int? ?: 0, days = dates as Int? ?: 0,
    hours = hours as Int? ?: 0, minutes = minutes as Int? ?: 0, seconds = seconds as Int? ?: 0
)

/** Normalize only rule-generated rollover, after explicit dates have been validated. */
private fun resolve(fields: TimeList, reference: LocalDateTime): LocalDateTime {
    val year = fields.years as Int? ?: reference.year
    val month = fields.months as Int? ?: (reference.month.number - 1)
    val day = fields.dates as Int? ?: reference.day
    var date = LocalDate(year, 1, 1).plus(month, DateTimeUnit.MONTH).plus(day - 1, DateTimeUnit.DAY)
    val total = (fields.hours as Int? ?: reference.hour).toLong() * 3600 +
        (fields.minutes as Int? ?: reference.minute).toLong() * 60 + (fields.seconds as Int? ?: reference.second)
    val extraDays = total.floorDiv(86400)
    date = date.plus(extraDays.toInt(), DateTimeUnit.DAY)
    val remainder = total.mod(86400)
    return LocalDateTime(date, LocalTime((remainder / 3600).toInt(), (remainder / 60 % 60).toInt(), (remainder % 60).toInt()))
}

private fun validExplicitDate(fields: TimeList, reference: LocalDateTime): Boolean {
    if (!fields.hasExplicitDate) return true
    return runCatching {
        LocalDate(fields.years as Int? ?: reference.year, (fields.months as Int? ?: (reference.month.number - 1)) + 1, fields.dates as Int? ?: reference.day)
    }.isSuccess
}

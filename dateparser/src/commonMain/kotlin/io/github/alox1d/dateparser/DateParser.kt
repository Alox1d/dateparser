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
    ): List<DateMatch> = parseNormalized(text, reference, timeZone, options, null)

    /** Choose the field order explicitly for slash dates such as 03/04/2026. */
    public fun parse(
        text: String,
        dateOrder: DateOrder,
        reference: Instant = clock.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        options: ParseOptions = ParseOptions()
    ): List<DateMatch> = parseNormalized(text, reference, timeZone, options, dateOrder)

    private fun parseNormalized(
        text: String,
        reference: Instant,
        timeZone: TimeZone,
        options: ParseOptions,
        dateOrder: DateOrder?
    ): List<DateMatch> {
        if (text.isBlank()) return emptyList()
        val localReference = reference.toLocalDateTime(timeZone)
        val normalized = EnglishGrammar.normalize(text, dateOrder, localReference.date)
        if (normalized == EnglishGrammar.REJECTED) return emptyList()
        val fields = try {
            ParseEngine(localReference).parseDate(normalized, options.errorLimit, options.minimumPrevalence)
        } catch (_: NumberFormatException) {
            return emptyList()
        }
        return try {
            fields.mapNotNull { parsed ->
                if (!validExplicitDate(parsed.target_date, localReference)) return@mapNotNull null
                val recurrence = parsed.period_time.takeIf { it.hasFields() }?.toPeriod()
                if (recurrence != null && !parsed.period_time.isPositivePeriod()) return@mapNotNull null
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
                val start = instant.toLocalDateTime(timeZone)
                val endInstant = parsed.max_date.takeIf { it.hasFields() }?.let { fields ->
                    if (!validExplicitDate(fields, start)) return@mapNotNull null
                    var end = resolve(fields, start).toInstant(timeZone)
                    if (end < instant) {
                        if (fields.hasExplicitDate) return@mapNotNull null
                        end = resolve(fields, LocalDateTime(start.date.plus(1, DateTimeUnit.DAY), start.time)).toInstant(timeZone)
                    }
                    if (end < instant) return@mapNotNull null
                    end
                }
                DateMatch(
                    start, instant, timeZone, recurrence,
                    endInstant?.toLocalDateTime(timeZone), endInstant, parsed.toString(), parsed.confidence
                )
            }
        } catch (_: DateTimeArithmeticException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}

private fun TimeList.hasFields(): Boolean = listOf(years, months, dates, hours, minutes, seconds).any { it != null }
private fun TimeList.isPositivePeriod(): Boolean {
    val amounts = listOf(years, months, dates, hours, minutes, seconds).map { it as Int? ?: 0 }
    return amounts.all { it >= 0 } && amounts.any { it > 0 }
}
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

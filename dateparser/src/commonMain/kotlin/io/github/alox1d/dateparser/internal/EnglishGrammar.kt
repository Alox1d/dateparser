package io.github.alox1d.dateparser.internal

import io.github.alox1d.dateparser.DateOrder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.number

/** Translate recognized English forms into the shared rule grammar, retaining surrounding text. */
internal object EnglishGrammar {
    const val REJECTED = "zzunresolvedtemporalexpressionzz"
    private val ignoreCase = RegexOption.IGNORE_CASE
    private val monthNames = listOf(
        listOf("january", "jan"), listOf("february", "feb"), listOf("march", "mar"),
        listOf("april", "apr"), listOf("may"), listOf("june", "jun"), listOf("july", "jul"),
        listOf("august", "aug"), listOf("september", "sept", "sep"), listOf("october", "oct"),
        listOf("november", "nov"), listOf("december", "dec")
    )
    private val months = monthNames.flatten().sortedByDescending { it.length }.joinToString("|")
    private val start = "(?<![\\p{L}\\d])"
    private val end = "(?![\\p{L}\\d])"
    private val weekdayNames = "monday|tuesday|wednesday|thursday|friday|saturday|sunday"
    private val namedPrefix = "(?:(?:on\\s+)?($weekdayNames)\\s*,?\\s*)?"
    private val monthFirst = Regex("$start$namedPrefix($months)\\.?\\s*([+-]?\\d+)(st|nd|rd|th)?(?:\\s*,?\\s*(\\d{4,}))?$end", ignoreCase)
    private val dayFirst = Regex("$start$namedPrefix([+-]?\\d+)(st|nd|rd|th)?\\s*($months)\\.?(?:\\s*,?\\s*(\\d{4,}))?$end", ignoreCase)
    private val slashDate = Regex("$start(\\d{1,6})/(\\d{1,3})(?:/(\\d{1,6}))?$end")
    private val isoDate = Regex("$start(\\d{4,})-(\\d{1,3})-(\\d{1,3})$end")
    private val numericCandidate = Regex("$start[+-]?\\d+(?:[/:-]\\d+)+$end")
    private val amPmCandidate = Regex("$start[+-]?\\d+(?::\\d*)*\\s*[ap]\\.?m\\.?$end", ignoreCase)
    private val amPm = Regex("$start(\\d{1,3})(?::(\\d{1,3}))?(?::(\\d{1,3}))?\\s*([ap])\\.?m\\.?$end", ignoreCase)
    private val nextPeriod = Regex("\\bnext\\s+(week|month|year)s?\\b", ignoreCase)
    private val dottedDate = Regex("$start(?<!\\d\\.)([+-]?\\d{1,2})\\.(\\d{2})\\.(\\d{1,4})(?!\\.\\d)$end")
    private val weekday = Regex("\\b(?:(on|next|every)\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", ignoreCase)
    private val firstWeekdayNextMonth = Regex(
        "$start(?:(every|each)\\s+)?(?:the\\s+)?(first|second|third|fourth|fifth|last|\\d+(?:st|nd|rd|th))\\s+($weekdayNames)\\s+of\\s+next\\s+month$end",
        ignoreCase
    )
    private val unit = "(?:years?|months?|weeks?|days?|hours?|minutes?|seconds?)"
    private val compound = Regex("\\b(?:in|after|every)\\s+[+-]?\\d+\\s+$unit(?:\\s+and\\s+[+-]?\\d+\\s+$unit)+\\b", ignoreCase)
    private val conjunction = Regex("\\s+and\\s+", ignoreCase)

    fun normalize(text: String, dateOrder: DateOrder?, referenceDate: LocalDate = LocalDate(2000, 1, 1)): String {
        // An unresolved bare hour must not silently inherit the reference clock.
        // Only explicit AM/PM or minutes disambiguate these otherwise partial matches.
        val ambiguousAtHour = Regex("\\bat\\s+\\d{1,3}\\b(?!\\s*(?:[:.]\\d|[ap]\\.?m\\.?))", ignoreCase)
        val trailingDateHour = Regex(
            "\\b(?:today|tomorrow|tomorow|yesterday)\\s+\\d{1,3}\\b(?!\\s*(?:[:.]\\d|[ap]\\.?m\\.?))(?=\\s*(?:$|[,;!?]|\\p{So}))",
            ignoreCase
        )
        if (ambiguousAtHour.containsMatchIn(text) || trailingDateHour.containsMatchIn(text)) return REJECTED
        val referenceYear = referenceDate.year
        var rejected = false
        fun date(dayText: String, month: Int, yearText: String = "", ordinal: String = "", namedWeekday: String = ""): String {
            val day = dayText.toIntOrNull()
            val year = if (yearText.isEmpty()) referenceYear else yearText.toIntOrNull()
            val suffix = when {
                day == null -> ""
                day % 100 in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            if (dayText.startsWith("-") || dayText.startsWith("+") || day == null || year == null || year !in 1..9999 ||
                (ordinal.isNotEmpty() && ordinal.lowercase() != suffix) ||
                runCatching { LocalDate(year, month, day) }.isFailure) {
                rejected = true
                return REJECTED
            }
            if (namedWeekday.isNotEmpty() && LocalDate(year, month, day).dayOfWeek != DayOfWeek.valueOf(namedWeekday.uppercase())) {
                rejected = true
                return REJECTED
            }
            val result = "$day.${month.toString().padStart(2, '0')}"
            return " " + (if (yearText.isEmpty()) result else "$result.${year.toString().padStart(4, '0')}") + " "
        }
        fun month(name: String) = monthNames.indexOfFirst { name.lowercase() in it } + 1
        for (candidate in numericCandidate.findAll(text)) {
            if ('/' in candidate.value && !slashDate.matches(candidate.value) ||
                '-' in candidate.value && candidate.value.count { it == '-' } >= 2 && !isoDate.matches(candidate.value)) {
                return REJECTED
            }
        }
        for (candidate in amPmCandidate.findAll(text)) {
            if (!amPm.matches(candidate.value)) return REJECTED
        }
        for (candidate in dottedDate.findAll(text)) {
            date(candidate.groupValues[1], candidate.groupValues[2].toIntOrNull() ?: 0, candidate.groupValues[3])
        }
        for (candidate in compound.findAll(text)) {
            if (Regex("[+-]?\\d+").findAll(candidate.value).any { (it.value.toIntOrNull() ?: 0) <= 0 }) return REJECTED
        }
        if (rejected) return REJECTED
        var result = dayFirst.replace(text) { m -> date(m.groupValues[2], month(m.groupValues[4]), m.groupValues[5], m.groupValues[3], m.groupValues[1]) }
        // A title such as "Call May 1st Monday ..." must not turn "May 1st" into a named date.
        val ordinalRanges = firstWeekdayNextMonth.findAll(result).map { it.range }.toList()
        result = monthFirst.replace(result) { m ->
            if (ordinalRanges.any { it.first <= m.range.last && m.range.first <= it.last }) m.value
            else date(m.groupValues[3], month(m.groupValues[2]), m.groupValues[5], m.groupValues[4], m.groupValues[1])
        }
        result = firstWeekdayNextMonth.replace(result) { m ->
            val monthStart = runCatching {
                LocalDate(referenceDate.year, referenceDate.month.number, 1).plus(1, DateTimeUnit.MONTH)
            }.getOrNull()
            if (m.groupValues[1].isNotEmpty() || m.groupValues[2].lowercase() !in listOf("first", "1st") || monthStart == null) {
                rejected = true
                REJECTED
            } else {
                val dayOfWeek = DayOfWeek.valueOf(m.groupValues[3].uppercase())
                val offset = (dayOfWeek.ordinal - monthStart.dayOfWeek.ordinal + 7) % 7
                val resolved = monthStart.plus(offset, DateTimeUnit.DAY)
                date(resolved.day.toString(), resolved.month.number, resolved.year.toString())
            }
        }
        if (rejected) return REJECTED
        result = isoDate.replace(result) { m -> date(m.groupValues[3], m.groupValues[2].toInt(), m.groupValues[1]) }
        result = slashDate.replace(result) { m ->
            val first = m.groupValues[1].toInt()
            val second = m.groupValues[2].toInt()
            val third = m.groupValues[3]
            val order = dateOrder ?: when {
                first in 13..31 && second in 1..12 -> DateOrder.DMY
                second in 13..31 && first in 1..12 -> DateOrder.MDY
                first == second && first in 1..12 -> DateOrder.DMY
                else -> null
            }
            when (order) {
                DateOrder.DMY -> date(m.groupValues[1], second, third)
                DateOrder.MDY -> date(m.groupValues[2], first, third)
                DateOrder.YMD -> if (third.isEmpty()) { rejected = true; REJECTED } else date(third, second, m.groupValues[1])
                null -> { rejected = true; REJECTED }
            }
        }
        result = amPm.replace(result) { m ->
            val hour = m.groupValues[1].toIntOrNull()
            val minute = m.groupValues[2].ifEmpty { "0" }.toInt()
            val second = m.groupValues[3].ifEmpty { "0" }.toInt()
            if (hour == null || hour !in 1..12 || minute !in 0..59 || second !in 0..59) {
                rejected = true
                REJECTED
            } else {
                val hours = hour % 12 + if (m.groupValues[4].equals("p", true)) 12 else 0
                " " + listOf(hours, minute, second).joinToString(":") { it.toString().padStart(2, '0') } + " "
            }
        }
        if (rejected) return REJECTED
        result = nextPeriod.replace(result) { "in 1 ${it.groupValues[1].lowercase()}" }
        result = weekday.replace(result) { if (it.groupValues[1].isEmpty()) "on ${it.value}" else it.value }
        return compound.replace(result) { conjunction.replace(it.value, " ").replace("+", "") }
    }
}

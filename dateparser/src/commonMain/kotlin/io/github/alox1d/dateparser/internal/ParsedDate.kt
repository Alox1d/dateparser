package io.github.alox1d.dateparser.internal

import kotlinx.datetime.*

internal class ParsedDate(
    val target_date: TimeList,

    val period_time: TimeList,

    val max_date: TimeList,

    var string: Any?,
    val confidence: Int, val contexts: ArrayList<Int>,
    val relativePeriod: DateTimePeriod? = null
) {

override fun toString(): String = string.toString()
}

internal class ParseEngine(reference: LocalDateTime) {
    private val dateCases = DateCases(reference)
        private val MaxDistanceBetweenWords = 3;

        fun distanceBetweenParsedTimeAndTimeList(PT: ParsedTime, TS: TimeSet): Int {
            val timeList = mapTimeList(PT, TS)
            val indexes = listOf(timeList.seconds, timeList.minutes, timeList.hours,
                timeList.dates, timeList.months, timeList.years)
                .filterIsInstance<ParsedTime>().flatMap { it.indexes }
            if (indexes.isEmpty() || PT.indexes.isEmpty()) return 0
            return maxOf(0, indexes.minOrNull()!! - PT.indexes.maxOrNull()!!,
                PT.indexes.minOrNull()!! - indexes.maxOrNull()!!)
        }

        fun clearNotValidParsedTimes(parsedTimes: ArrayList<ParsedTime>): ArrayList<ParsedTime> {
            var i = parsedTimes.size
            while (i-- > 0) {
                if (parsedTimes[i].validMode == ValidModes.notValid) {

                    parsedTimes.removeAt(i)

                }
            }
            return parsedTimes;
        }

        fun findSuitableTimeList(
            timeSets: ArrayList<TimeSet>,
            parsedTime: ParsedTime,
            contextsData: ContextsData
        ): Int {
            var index = 0;
            for (timeSet in timeSets) {
                if (distanceBetweenParsedTimeAndTimeList(
                        parsedTime,
                        timeSet
                    ) <= MaxDistanceBetweenWords
                    && parsedTime.context == timeSet.context
                ) {

                    val timeList = mapTimeList(parsedTime, timeSet)

                    var timeProperty: Any? = mapTimeType(parsedTime, timeList)

                    if (timeProperty == null) {
                        timeProperty = parsedTime;
                        break;
                    }

                }
                index++;
            }
            return index;
        }

        private fun mapTimeList(
            parsedTime: ParsedTime,
            timeSet: TimeSet
        ): TimeList {
            return when (parsedTime.dateType) {
                DateTypes.target_date -> {
                    timeSet.target_date
                }
                DateTypes.period_time -> {
                    timeSet.period_time
                }
                DateTypes.max_date -> {
                    timeSet.max_date
                }
            }
        }

        private fun addListToSet(
            parsedTime: ParsedTime,
            timeSet: TimeSet, timeList: TimeList
        ) {
            when (parsedTime.dateType) {
                DateTypes.target_date -> {
                    timeSet.target_date = timeList
                }
                DateTypes.period_time -> {
                    timeSet.period_time = timeList
                }
                DateTypes.max_date -> {
                    timeSet.max_date = timeList
                }
            }
        }

        private fun mapTimeType(
            parsedTime: ParsedTime,
            timeList: TimeList
        ): Any? {
            return when (parsedTime.timeType) {
                TimeTypes.seconds -> {
                    timeList.seconds
                }
                TimeTypes.minutes -> timeList.minutes

                TimeTypes.hours -> timeList.hours
                TimeTypes.dates -> timeList.dates
                TimeTypes.months -> timeList.months
                TimeTypes.years -> timeList.years
                else -> null
            }
        }

        private fun addParsedToList(
            parsedTime: ParsedTime,
            timeList: TimeList
        ) {
            if (parsedTime.isExplicitDate) timeList.hasExplicitDate = true
            if (parsedTime.isFixed) when (parsedTime.timeType) {
                TimeTypes.hours -> timeList.fixedHours = parsedTime.number
                TimeTypes.minutes -> timeList.fixedMinutes = parsedTime.number
                TimeTypes.seconds -> timeList.fixedSeconds = parsedTime.number
                else -> Unit
            }
            when (parsedTime.timeType) {
                TimeTypes.seconds ->
                    timeList.seconds = parsedTime
                TimeTypes.minutes -> timeList.minutes = parsedTime

                TimeTypes.hours -> timeList.hours = parsedTime
                TimeTypes.dates -> timeList.dates = parsedTime
                TimeTypes.months, TimeTypes.nMonth -> timeList.months = parsedTime
                TimeTypes.years -> timeList.years = parsedTime
            }
        }
            
            fun addNewParsedTime(
                timeSets: ArrayList<TimeSet>,
                parsedTime: ParsedTime,
                contextsData: ContextsData
            ) {
                val index = findSuitableTimeList(timeSets, parsedTime, contextsData);
                if (index == timeSets.size) {
                    var timeSet = TimeSet(parsedTime.context);
                    val timeList = TimeList();

                    addParsedToList(parsedTime, timeList)

                    addListToSet(parsedTime, timeSet, timeList)

                    timeSets.add(timeSet);
                    var curTimeList: TimeList? = null
                    if (parsedTime.isOffset) {
                        curTimeList = mapTimeList(parsedTime, timeSets[timeSets.size - 1])
                        curTimeList.isOffset = true;
                    }
                    if (parsedTime.isFixed) {
                        curTimeList = mapTimeList(parsedTime, timeSets[timeSets.size - 1])
                        curTimeList.isFixed = true;

                    }
                } else {
                    val curTimeList: TimeList = mapTimeList(parsedTime, timeSet = timeSets[index])

                    addParsedToList(parsedTime, curTimeList)

                    if (parsedTime.isOffset) {
                        curTimeList.isOffset = true;

                    }
                    if (parsedTime.isFixed) {
                        curTimeList.isFixed = true;

                    }
                }
            }

            fun FixComposedWordsEdges(source: ArrayList<Expression?>): ArrayList<Expression?> {
                val words = ArrayList(source)
                fun conjunction(word: Expression?) = word?.regex_char in listOf("!", "I")
                while (words.isNotEmpty() && conjunction(words.first())) words.removeAt(0)
                while (words.isNotEmpty() && conjunction(words.last())) words.removeAt(words.lastIndex)
                words.lastOrNull()?.takeIf { it.is_separator }?.let { it.text = it.text.dropLast(1) }
                return words
            }

            fun parseDate(
                string: String,
                errorLimit: Double = 1.0,
                minimumPrevalence: Double = 50.0
            ):ArrayList<ParsedDate> {

                val expressions = RegexExtractor.extractRegexChars(string, errorLimit);

                val expressionsForExtracting = ArrayList<Expression>()
                for (expr in expressions) {
                    if (expr != null) {
                        expressionsForExtracting.add(expr.copy())
                    };
                }
                val casesResult = dateCases.extractTime(expressionsForExtracting, minimumPrevalence);
                var parsedTimes = casesResult.parsedTimes;
                parsedTimes = clearNotValidParsedTimes(parsedTimes);

                parsedTimes.sortBy {
                    it.indexes[0]
                }
                val contextsData = casesResult.contextsData;
                for (index in casesResult.separatingWords) {
                    expressions[index]?.is_separator = true;
                }
                var i = contextsData.contexts?.size!!;
                while (i-- > 0) {
                    val context = contextsData.contexts!![i];
                    for (j in context.start..context.end) {
                        expressions[j]?.context = i;
                    }
                }
                val timeSets = ArrayList<TimeSet>();
                for (iParsedTime in parsedTimes) {
                    addNewParsedTime(timeSets, iParsedTime, contextsData);
                }
                val parsedDates = ArrayList<ParsedDate>();
                contextsData.usedContexts?.sortBy { it }

                for (timeSet in timeSets) {
                    var confidence = 0;
                    val indexes = ArrayList<Int>();

                    val max_date: TimeList = timeSet.max_date
                    if (max_date.seconds != null) {
                        val time = (max_date.seconds as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (max_date.minutes != null) {
                        val time = (max_date.minutes as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (max_date.hours != null) {
                        val time = (max_date.hours as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (max_date.dates != null) {
                        val time = (max_date.dates as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (max_date.months != null) {
                        val time = (max_date.months as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (max_date.years != null) {
                        val time = (max_date.years as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }

                    val target_date: TimeList = timeSet.target_date
                    if (target_date.seconds != null) {
                        val time = (target_date.seconds as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (target_date.minutes != null) {
                        val time = (target_date.minutes as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (target_date.hours != null) {
                        val time = (target_date.hours as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (target_date.dates != null) {
                        val time = (target_date.dates as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (target_date.months != null) {
                        val time = (target_date.months as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (target_date.years != null) {
                        val time = (target_date.years as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }

                    val period_time: TimeList = timeSet.period_time
                    if (period_time.seconds != null) {
                        val time = (period_time.seconds as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (period_time.minutes != null) {
                        val time = (period_time.minutes as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (period_time.hours != null) {
                        val time = (period_time.hours as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (period_time.dates != null) {
                        val time = (period_time.dates as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (period_time.months != null) {
                        val time = (period_time.months as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }
                    if (period_time.years != null) {
                        val time = (period_time.years as ParsedTime)
                        indexes.addAll(time.indexes);
                        confidence += time.prevalence;
                    }

                    val contexts =
                        ArrayList<Int>();
                    val composedWords = ArrayList<Expression?>();
                    val context = contextsData.contexts!![timeSet.context];
                    val start = context.start;
                    val end = context.end;
                    for (j in start..end) {
                        val expression = expressions [j];
                        val ii = indexes.indexOf(j)
                        if (indexes.indexOf(j) == -1) {
                            composedWords.add(expression);
                            if (!contexts.contains(expression?.context)) {
                                if (expression != null) {
                                    contexts.add(expression.context)
                                };
                            }
                        }
                    }
                    for ( composedWord in composedWords) {
                        if (composedWord != null) {
                                if (!contextsData.usedContexts?.contains(composedWord.context)!!) {
                                    contextsData.usedContexts!!.add(composedWord.context);
                            }

                        }
                    }
                    parsedDates.add(
                        ParsedDate (
                                timeSet.target_date,
                        timeSet.period_time,
                        timeSet.max_date,
                        composedWords,
                        confidence,
                        contexts,
                        contextsData.relativePeriods[timeSet.context]
                    ));
                }
                val notUsedContexts = ArrayList<Int>()
                i = contextsData.contexts!!.size;
                while (i-- > 0) {
                    notUsedContexts.add(i);
                }
                for (usedContextIndex in contextsData.usedContexts!!) {

                    notUsedContexts.removeAt(notUsedContexts.indexOf(usedContextIndex))

                }
                val separators = Loader.LoadSeparators();
                ContextHandling.ContextHandling.proceedNotUsedContexts(
                    parsedDates,
                    separators,
                    notUsedContexts,
                    contextsData,
                    expressions
                );
                for ( parsedDate in parsedDates) {
                    var composedString = "";
                    var composedWords = parsedDate . string as ArrayList<Expression?>;
                    if (composedWords.size > 0) {
                        composedWords = FixComposedWordsEdges(composedWords);
                        composedWords.forEach{
                            composedString += it!!.text + " ";
                        }
                    }

                    parsedDate.string = composedString.trim()

                    val max_date: TimeList = parsedDate.max_date

                    if (max_date.months != null) {
                        val time = (max_date.months as ParsedTime)
                        if (time != null && time.number != null){
                            time.number--;
                        }
                        max_date.months = time.number
                    }
                    if (max_date.seconds != null) {
                        val time = (max_date.seconds as ParsedTime)
                        max_date.seconds = time.number

                    }
                    if (max_date.minutes != null) {
                        val time = (max_date.minutes as ParsedTime)
                        max_date.minutes = time.number

                    }
                    if (max_date.hours != null) {
                        val time = (max_date.hours as ParsedTime)
                        max_date.hours = time.number

                    }
                    if (max_date.dates != null) {
                        val time = (max_date.dates as ParsedTime)
                        max_date.dates = time.number

                    }

                    if (max_date.years != null) {
                        val time = (max_date.years as ParsedTime)
                        max_date.years = time.number

                    }

                    val target_date: TimeList = parsedDate.target_date

                    if (target_date.months != null) {
                        val time = (target_date.months as ParsedTime)
                        if (time != null && time.number != null){
                            time.number--;
                        }
                        target_date.months = time.number
                    }
                    if (target_date.seconds != null) {
                        val time = (target_date.seconds as ParsedTime)
                        target_date.seconds = time.number

                    }
                    if (target_date.minutes != null) {
                        val time = (target_date.minutes as ParsedTime)
                        target_date.minutes = time.number

                    }
                    if (target_date.hours != null) {
                        val time = (target_date.hours as ParsedTime)
                        target_date.hours = time.number

                    }
                    if (target_date.dates != null) {
                        val time = (target_date.dates as ParsedTime)
                        target_date.dates = time.number

                    }

                    if (target_date.years != null) {
                        val time = (target_date.years as ParsedTime)
                        target_date.years = time.number

                    }

                    val period_time: TimeList = parsedDate.period_time

                    if (period_time.months != null) {
                        period_time.months = (period_time.months as ParsedTime).number
                    }
                    if (period_time.seconds != null) {
                        val time = (period_time.seconds as ParsedTime)
                        period_time.seconds = time.number

                    }
                    if (period_time.minutes != null) {
                        val time = (period_time.minutes as ParsedTime)
                        period_time.minutes = time.number

                    }
                    if (period_time.hours != null) {
                        val time = (period_time.hours as ParsedTime)
                        period_time.hours = time.number

                    }
                    if (period_time.dates != null) {
                        val time = (period_time.dates as ParsedTime)
                        period_time.dates = time.number

                    }

                    if (period_time.years != null) {
                        val time = (period_time.years as ParsedTime)
                        period_time.years = time.number

                    }

                }
                return parsedDates;
            }
        }
internal class TimeList() {
        var hasExplicitDate: Boolean = false
        var fixedHours: Int? = null
        var fixedMinutes: Int? = null
        var fixedSeconds: Int? = null
        var seconds: Any? = null
        var minutes: Any? = null
        var hours: Any? = null
        var dates: Any? = null
        var months: Any? = null
        var years: Any? = null
        var isOffset: Boolean? = null
        var isFixed: Boolean? = null

    }

internal class TimeSet(val context: Int) {
        var target_date: TimeList = TimeList()
        var period_time: TimeList = TimeList()
        var max_date: TimeList = TimeList()

    }

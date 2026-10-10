package com.huawei.wordnote.domain.services.dateparser

import android.content.Context
import com.alox1d.dateparser.dateparser.ContextHandling
import ru.alox1d.wordnote.huawei.domain.services.dateparser.*
import ru.alox1d.wordnote.huawei.domain.services.dateparser.DateCases.Companion.extractTime
import ru.alox1d.wordnote.huawei.domain.services.dateparser.DateCases.Companion.getDateProperty
import ru.alox1d.wordnote.huawei.domain.services.dateparser.RegexExtractor.Companion.extractRegexChars
import java.util.*
import kotlin.collections.ArrayList

class ParsedDate(
    val target_date: TimeList,
//    val target_date_num: Int,
    val period_time: TimeList,
//    val period_time_num: Int,
    val max_date: TimeList,
//    val max_date_num: Int,
    var string: Any?,
    val confidence: Int, val contexts: ArrayList<Int>
) {

    class DateParserModel(
        val target_date: Calendar, val period_time: Long,
        val max_date: Long
    )

    /**
     * @param {Date} target_date
     * @param {Number} period_time
     * @param {Date} max_date
     * @param {String} string
     * @param {Number} confidence
     * @param {Array.<Number>} contexts
     */
//    constructor() {
//        this.target_date = target_date;
//        this.period_time = period_time;
//        this.max_date = max_date;
//        this.string = string;
//        this.confidence = confidence;
//        this.contexts = contexts;
//    }

    /**@returns {String} */
    override fun toString(): String {
        return string.toString();
    }

    /**@returns {{target_date: Date, period_time: Date, max_date: Date}} */
//    fun valueOf() {
    fun valueOf(): DateParserModel {
        val now = Calendar.getInstance()
        fun absolute(fields: TimeList): Calendar = Calendar.getInstance().apply {
            clear()
            set(
                (fields.years as Int?) ?: now.get(Calendar.YEAR),
                (fields.months as Int?) ?: now.get(Calendar.MONTH),
                (fields.dates as Int?) ?: now.get(Calendar.DAY_OF_MONTH),
                (fields.hours as Int?) ?: now.get(Calendar.HOUR_OF_DAY),
                (fields.minutes as Int?) ?: now.get(Calendar.MINUTE),
                (fields.seconds as Int?) ?: now.get(Calendar.SECOND)
            )
        }
        // A recurrence is a duration, not a local date. Preserve the upstream
        // convention of measuring month/year intervals from the UTC epoch.
        val duration = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = 0
            add(Calendar.YEAR, (period_time.years as Int?) ?: 0)
            add(Calendar.MONTH, (period_time.months as Int?) ?: 0)
            add(Calendar.DAY_OF_MONTH, (period_time.dates as Int?) ?: 0)
            add(Calendar.HOUR_OF_DAY, (period_time.hours as Int?) ?: 0)
            add(Calendar.MINUTE, (period_time.minutes as Int?) ?: 0)
            add(Calendar.SECOND, (period_time.seconds as Int?) ?: 0)
        }.timeInMillis
        return DateParserModel(absolute(target_date), duration, absolute(max_date).timeInMillis)
    }


    companion object {
        lateinit var dateCases: DateCases
        lateinit var androidContext: Context

        fun prepareParse(context: Context) {
            dateCases = DateCases.getInstance(context)
            androidContext = context
        }

        const val MaxDistanceBetweenWords = 3;

        /**
         * @param {ParsedTime} PT
         * @param {TimeSet} TS
         * @returns {Number}
         */
        fun distanceBetweenParsedTimeAndTimeList(PT: ParsedTime, TS: TimeSet): Int {
            val timeList = mapTimeList(PT, TS)
            val indexes = listOf(timeList.seconds, timeList.minutes, timeList.hours,
                timeList.dates, timeList.months, timeList.years)
                .filterIsInstance<ParsedTime>().flatMap { it.indexes }
            if (indexes.isEmpty() || PT.indexes.isEmpty()) return 0
            return maxOf(0, indexes.minOrNull()!! - PT.indexes.maxOrNull()!!,
                PT.indexes.minOrNull()!! - indexes.maxOrNull()!!)
        }

        /**
         * @param {Array.<ParsedTime>} parsedTimes
         * @returns {Array.<ParsedTime>}
         */
        fun clearNotValidParsedTimes(parsedTimes: ArrayList<ParsedTime>): ArrayList<ParsedTime> {
            var i = parsedTimes.size
            while (i-- > 0) {
                if (parsedTimes[i].validMode == ValidModes.notValid) {
//                parsedTimes.splice(i, 1);
                    parsedTimes.removeAt(i)

                }
            }
            return parsedTimes;
        }

        /**
         * @this {Array.<TimeSet>}
         * @param {ParsedTime} parsedTime
         * @param {ContextsData} contextsData
         * @returns {Number} Index of suitable list
         */
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


//            if (timeSet[parsedTime.dateType].hasOwnProperty(parsedTime.timeType))
//            {

                    if (timeProperty == null) {
                        timeProperty = parsedTime;
                        break;
                    }
//            }
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
            /**
             * @this {Array.<TimeSet>}
             * @param {ParsedTime} parsedTime
             * @param {ContextsData} contextsData
             */
            fun addNewParsedTime(
                timeSets: ArrayList<TimeSet>,
                parsedTime: ParsedTime,
                contextsData: ContextsData
            ) {
                val index = findSuitableTimeList(timeSets, parsedTime, contextsData);
                if (index == timeSets.size) {
                    var timeSet = TimeSet(parsedTime.context);
                    val timeList = TimeList();

//            val timeList = mapTimeList(parsedTime, timeList)
//            var timeProperty:Any? = mapTimeType(parsedTime, timeList)

                    addParsedToList(parsedTime, timeList)
//            timeList[parsedTime.timeType] = parsedTime;

//            timeSet[parsedTime.dateType] = timeList
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
//                this[this.length - 1][parsedTime.dateType].isFixed = true;
                    }
                } else {
                    val curTimeList: TimeList = mapTimeList(parsedTime, timeSet = timeSets[index])
//                    val curTimeProperty = mapTimeType(parsedTime, curTimeList)
//            var curTimeType = mapTimeType(parsedTime,curTimeList)
                    addParsedToList(parsedTime, curTimeList)
//            this[index][parsedTime.dateType][parsedTime.timeType] = parsedTime;
                    if (parsedTime.isOffset) {
                        curTimeList.isOffset = true;
//                this[index][parsedTime.dateType].isOffset = true;
                    }
                    if (parsedTime.isFixed) {
                        curTimeList.isFixed = true;
//                this[index][parsedTime.dateType].isFixed = true;
                    }
                }
            }


            /**
             * @param {Array.<Expression>} composedWords
             * @returns {Array.<Expression>}
             */
            fun FixComposedWordsEdges(_composedWords:ArrayList<Expression?>):ArrayList<Expression?> {
                var composedWords = _composedWords
                if (composedWords[0]!!.regex_char == "!" || composedWords[0]!!.regex_char == "I") {
                    composedWords = composedWords.slice(1..composedWords.size-1) as ArrayList<Expression?>;
                }
                if (composedWords.size > 0) {
                    var end = composedWords . size -1;
                    var lastWord = composedWords [end];
                    if (lastWord!!.regex_char == "!" || lastWord!!.regex_char == "I") {
                        composedWords = composedWords.slice(0 until end) as ArrayList<Expression?>;
                        end = composedWords.size - 1;
                        lastWord = composedWords[end];
                    }
                    if (lastWord!!.is_separator) {
                        lastWord.text = lastWord.text.substring(0, lastWord.text.length - 1);
                    }
                }
                return composedWords;
            }

            /**
             * Extracts date from string with defined precision.
             * @param {String} string
             * @param {Number} errorLimit From 0.0 to 1.0, the less — the less results
             * @param {Number} minimumPrevalence From 0 to 100, the less — the more results
             * @returns {Array.<ParsedDate>} parsed date.
             */
            fun parseDate(
                string: String,
                errorLimit: Double = 1.0,
                minimumPrevalence: Double = 50.0
            ):ArrayList<ParsedDate> {
//        if (typeof (errorLimit) == 'undefined') { // ЕСЛИ UNDEFINED ТО ЗНАЧЕНИЕ ПО УМОЛЧАНИЮ
//            errorLimit = 1;
//        }
//        if (typeof (minimumPrevalence) == 'undefined') {
//            minimumPrevalence = 50;
//        }

                val expressions = extractRegexChars(androidContext, string, errorLimit);
//        val expressionsForExtracting = [];
                val expressionsForExtracting = ArrayList<Expression>()
                for (expr in expressions) {
                    if (expr != null) {
                        expressionsForExtracting.add(expr.copy())
                    };
                }
                val casesResult = extractTime(expressionsForExtracting, minimumPrevalence);
                var parsedTimes = casesResult.parsedTimes;
                parsedTimes = clearNotValidParsedTimes(parsedTimes);
//        parsedTimes.sort(function(a, b) {
//            return a.indexes[0] - b.indexes[0];
//        })
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
//        contextsData.usedContexts?.sortBy((a, b) => {
//            return a - b;
//        });
                for (timeSet in timeSets) {
                    var confidence = 0;
                    val indexes = ArrayList<Int>();
//            for (dateType in timeSet) {
//                if (isDateType(dateType) && timeSet.hasOwnProperty(dateType)) {
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
//                    const timeList = timeSet [dateType]
//                    for (timeProperty in timeList) {
//                        if (isTimeType(timeProperty) && timeList.hasOwnProperty(timeProperty)) {
//                            val time = timeList [timeProperty];
//                            if (typeof (time) != 'undefined') {
//                                indexes.push(... time . indexes);
//                                confidence += time.prevalence;
//                            }
//                        }
//                    }
//                }
//            }
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
                        contexts
                    ));
                }
                val notUsedContexts = ArrayList<Int>()
                i = contextsData.contexts!!.size;
                while (i-- > 0) {
                    notUsedContexts.add(i);
                }
                for (usedContextIndex in contextsData.usedContexts!!) {
//                    notUsedContexts.splice(notUsedContexts.indexOf(usedContextIndex), 1);
                    notUsedContexts.removeAt(notUsedContexts.indexOf(usedContextIndex))

                }
                val separators = Loader.getInstance(androidContext).LoadSeparators();
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


//                    for (timeType in parsedDate.target_date) {
//
//                        val value = parsedDate.target_date[timeType];
//                        if (typeof (value ) != 'undefined' && typeof (value.number) != 'undefined') {
//                            if (timeType == TimeTypes.months) {
//                                value .number--;
//                            }
//                            parsedDate.target_date[timeType] =
//                            val .number;
//                        }
//                    }
//                    for (const timeType in parsedDate.period_time) {
//                        let
//                        val = parsedDate.period_time[timeType];
//                        if (typeof (val ) != 'undefined' && typeof (val .number) != 'undefined') {
//                            if (timeType == TimeTypes.months) {
//                                val .number--;
//                            }
//                            parsedDate.period_time[timeType] =
//                            val .number;
//                        }
//                    }
//                    for (const timeType in parsedDate.max_date) {
//                        let
//                        val = parsedDate.max_date[timeType];
//                        if (typeof (val ) != 'undefined' && typeof (val .number) != 'undefined') {
//                            if (timeType == TimeTypes.months) {
//                                val .number--;
//                            }
//                            parsedDate.max_date[timeType] =
//                            val .number;
//                        }
//                    }
                }
                return parsedDates;
            }
        }
    }

    class TimeList() {
        var seconds: Any? = null
        var minutes: Any? = null
        var hours: Any? = null
        var dates: Any? = null
        var months: Any? = null
        var years: Any? = null
        var isOffset: Boolean? = null
        var isFixed: Boolean? = null
//        val timeTypes = mutableMapOf<TimeTypes, ParsedTime?>()
//        init {
//            timeTypes.put(TimeTypes.seconds, seconds)
//            timeTypes.put(TimeTypes.minutes, minutes)
//            timeTypes.put(TimeTypes.hours, hours)
//            timeTypes.put(TimeTypes.dates, dates)
//            timeTypes.put(TimeTypes.months, months)
//            timeTypes.put(TimeTypes.years, years)
//        }



//    fun Any.isAllNullInside(): Boolean {
//        if(this::class.TimeList.any { !it.returnType.isMarkedNullable }) return false
//        return this::class.TimeList.none { it.getter.call(this) != null }
//    }
    }

    class TimeSet(val context: Int) {
        var target_date: TimeList = TimeList()
        var period_time: TimeList = TimeList()
        var max_date: TimeList = TimeList()

    }

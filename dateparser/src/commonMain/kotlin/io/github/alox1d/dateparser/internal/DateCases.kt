package io.github.alox1d.dateparser.internal

import kotlinx.datetime.*

internal enum class DateTypes {
    target_date, period_time, max_date
}

internal enum class TimeTypes {
    seconds, minutes, hours, dates, months, nMonth, years
}

const val numberAndWordPrevalence = 50;

internal class DateCases(private val reference: LocalDateTime) {
    private val loader = Loader

    fun getDateProperty(date: LocalDateTime, timeType: TimeTypes, increaseMonth: Boolean): Int = when (timeType) {
        TimeTypes.seconds -> date.second
        TimeTypes.minutes -> date.minute
        TimeTypes.hours -> date.hour
        TimeTypes.dates -> date.day
        TimeTypes.months -> date.month.number - 1 + increaseMonth.toInt()
        TimeTypes.years -> date.year
        else -> -1
    }

    fun splitContext(exps: ArrayList<Expression>): SeparatingWords {
        val separators = loader?.LoadSeparators();

        val contexts = ArrayList<Context>()

        val separatingWords = ArrayList<Int>()

        var start = 0;
        var i = 0;
        while (i < exps.size) {
            val expression = exps[i];
            if (expression.regex_char == "!" || expression.regex_char == "I") {
                contexts.add(Context(start, i));
                start = i + 1;
            } else {
                for (separator in separators!!.expressions) {
                    val input = expression.text
                    val pattern = RulePattern.compile(separator.text)
                    val matches = pattern.matcher(input)

                    if (matches.length() == 1 && matches.findByIndex(0) == input.length - 1) {
                        expression.text = expression.text.substring(0, expression.text.length - 1);
                        contexts.add(Context(start, i));
                        start = i + 1;
                        separatingWords.add(i);

                    }

                }
            }
            i++
        }
        if (start < exps.size) {
            contexts.add(Context(start, exps.size - 1));
        }
        val sepWords = SeparatingWords(separatingWords, contexts)

        return sepWords
    }

    var finalParseCases = arrayListOf(

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: kotlin.Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("O")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    val allowedTypes =
                        arrayListOf(TimeTypes.hours, TimeTypes.minutes, TimeTypes.seconds)

                    parsedTimes.sortBy {
                        it.indexes[0]
                    }
                    var matchingParsedTimesIndexes = findAllMatchingParsedTimes(
                        allowedTypes,
                        -1,
                        index,
                        expressionSet.expressions,
                        parsedTimes
                    );
                    if (matchingParsedTimesIndexes.size == 0) {
                        matchingParsedTimesIndexes = findAllMatchingParsedTimes(
                            allowedTypes,
                            1,
                            index,
                            expressionSet.expressions,
                            parsedTimes
                        );
                    }
                    if (matchingParsedTimesIndexes.size > 0) {
                        var i = matchingParsedTimesIndexes.size;
                        var proceed = true;
                        while (proceed && i-- > 0) {
                            var parsedTime = parsedTimes[matchingParsedTimesIndexes[i]];
                            if (parsedTime.timeType == TimeTypes.hours) {
                                parsedTime.number =
                                    processHour(
                                        parsedTime.number,
                                        expressionSet.expressions[index].value.toInt()
                                    );
                                parsedTime.prevalence += prevalence;
                                proceed = false;
                            }
                            parsedTime.isFixed = true;
                        }
                        parsedTimes[matchingParsedTimesIndexes[i]].indexes.addAll(
                            markIndexes(
                                expressionSet,
                                index,
                                1,
                                true,
                                true
                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("C")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    val allowedTypes = arrayListOf(
                        TimeTypes.years,
                        TimeTypes.dates,
                        TimeTypes.nMonth,
                        TimeTypes.hours,
                        TimeTypes.minutes,
                        TimeTypes.seconds
                    )
                    parsedTimes.sortBy {
                        it.indexes[0]
                    }
                    var parsedTimesIndexes = findAllMatchingParsedTimes(
                        allowedTypes,
                        1,
                        index,
                        expressionSet.expressions,
                        parsedTimes
                    )
                    parsedTimesIndexes.sort();
                    if (parsedTimesIndexes.size > 0) {
                        var i2 = parsedTimesIndexes.size;
                        val now = reference;
                        val indexes = markIndexes(expressionSet, index, 1, true, false);
                        val context = processContexts(contextsData, indexes);
                        val offsets = parsedTimesIndexes.map { parsedTimes[it] }
                        fun amount(type: TimeTypes) = offsets.filter { it.timeType == type }.sumOf { it.number }
                        contextsData.relativePeriods[context] = DateTimePeriod(
                            years = amount(TimeTypes.years), months = amount(TimeTypes.nMonth),
                            days = amount(TimeTypes.dates), hours = amount(TimeTypes.hours),
                            minutes = amount(TimeTypes.minutes), seconds = amount(TimeTypes.seconds)
                        )
                        var notFoundTimeTypes =
                            arrayListOf(
                                TimeTypes.dates,
                                TimeTypes.hours,
                                TimeTypes.minutes,
                                TimeTypes.months,
                                TimeTypes.seconds,
                                TimeTypes.years
                            )
                        while (i2-- > 0) {
                            var parsedTime = parsedTimes[parsedTimesIndexes[i2]];
                            var index2 = notFoundTimeTypes.indexOf(parsedTime.timeType);
                            if (index2 != -1) {

                                notFoundTimeTypes.removeAt(index2)
                            }
                            if (parsedTime.timeType == TimeTypes.nMonth) {
                                parsedTimes.add(
                                    ParsedTime(
                                        DateTypes.target_date,
                                        TimeTypes.months,
                                        (now.month.number - 1) + 1 + parsedTime.number,
                                        parsedTime.indexes,
                                        context,
                                        prevalence,
                                        OptionModel(isOffset = true)

                                    )
                                );

                                parsedTimes.removeAt(parsedTimesIndexes[i2])

                                notFoundTimeTypes.removeAt(notFoundTimeTypes.indexOf(TimeTypes.months))

                            } else {
                                parsedTime.number =
                                    getDateProperty(
                                        now,
                                        parsedTime.timeType,
                                        true
                                    ) + parsedTime.number;
                            }
                            parsedTime.prevalence += prevalence;
                            parsedTime.indexes.addAll(indexes);
                            parsedTime.validMode = ValidModes.certified;
                        }
                        for (timeType in notFoundTimeTypes) {
                            parsedTimes.add(
                                ParsedTime(
                                    DateTypes.target_date,
                                    timeType,
                                    getDateProperty(now, timeType, true),
                                    indexes,
                                    context,
                                    prevalence,
                                    OptionModel(isOffset = true)
                                )
                            );
                        }
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("V\\.+I{0,1}[ZB]\\.+")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    val allowedTypes = arrayListOf(
                        TimeTypes.years,
                        TimeTypes.dates,
                        TimeTypes.months,
                        TimeTypes.hours,
                        TimeTypes.minutes,
                        TimeTypes.seconds
                    )
                    parsedTimes.sortBy {
                        it.indexes[0]
                    }
                    var fromParsedTimesIndexes = findAllMatchingParsedTimes(
                        allowedTypes,
                        1,
                        index,
                        expressionSet.expressions,
                        parsedTimes,
                        ValidModes.none
                    )
                    fromParsedTimesIndexes.sort();
                    if (fromParsedTimesIndexes.size > 0) {

                        val foundTimeTypes = ArrayList<TimeTypes>()
                        var proceed = true;
                        for (i in fromParsedTimesIndexes) {
                            var parsedTime = parsedTimes[i];
                            if (foundTimeTypes.contains(parsedTime.timeType)) {
                                proceed = false;
                                break;
                            } else {
                                foundTimeTypes.add(parsedTime.timeType);
                            }
                        }
                        if (proceed) {

                            val input = matches.group()
                            val pattern = RulePattern.compile("[ZB]")
                            val matches = pattern.matcher(input)
                            if (!matches.find()) continue
                            var zIndex = index + matches.start()

                            var topParsedTimesIndexes = findAllMatchingParsedTimes(
                                allowedTypes,
                                1,
                                zIndex,
                                expressionSet.expressions,
                                parsedTimes,
                                ValidModes.none
                            )
                            topParsedTimesIndexes.sort();
                            if (topParsedTimesIndexes.size > 0) {
                                var foundTimeTypes = ArrayList<TimeTypes>();
                                for (i in topParsedTimesIndexes) {
                                    var parsedTime = parsedTimes[i];
                                    if (foundTimeTypes.contains(parsedTime.timeType)) {
                                        proceed = false;
                                        break;
                                    } else {
                                        foundTimeTypes.add(parsedTime.timeType);
                                    }
                                }
                                if (proceed) {
                                    for (i in fromParsedTimesIndexes) {
                                        var parsedTime = parsedTimes[i];
                                        parsedTime.indexes.add(0, index);
                                        parsedTime.validMode = ValidModes.certified;
                                        parsedTime.prevalence += prevalence;
                                    }
                                    for (i in topParsedTimesIndexes) {
                                        var parsedTime = parsedTimes[i];
                                        parsedTime.dateType = DateTypes.max_date;
                                        parsedTime.indexes.add(0, zIndex);
                                        parsedTime.validMode = ValidModes.certified;
                                        parsedTime.prevalence += prevalence;
                                    }
                                }
                            }
                        }
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("Z\\.+")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    val allowedTypes = arrayListOf(
                        TimeTypes.years,
                        TimeTypes.dates,
                        TimeTypes.months,
                        TimeTypes.hours,
                        TimeTypes.minutes,
                        TimeTypes.seconds
                    )
                    parsedTimes.sortBy {
                        it.indexes[0]
                    }
                    var topParsedTimesIndexes = findAllMatchingParsedTimes(
                        allowedTypes,
                        1,
                        index,
                        expressionSet.expressions,
                        parsedTimes
                    )
                    topParsedTimesIndexes.sort();
                    var proceed = true;
                    if (topParsedTimesIndexes.size > 0) {
                        var foundTimeTypes = ArrayList<TimeTypes>()
                        for (i in topParsedTimesIndexes) {
                            var parsedTime = parsedTimes[i];
                            if (foundTimeTypes.contains(parsedTime.timeType)) {
                                proceed = false;
                                break;
                            } else {
                                foundTimeTypes.add(parsedTime.timeType);
                            }
                        }
                        if (proceed) {
                            for (i in topParsedTimesIndexes) {
                                var parsedTime = parsedTimes[i];
                                parsedTime.dateType = DateTypes.max_date;
                                parsedTime.indexes.add(0, index);
                                parsedTime.validMode = ValidModes.certified;
                                parsedTime.prevalence += prevalence;
                            }
                        }
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("E\\.+")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    val allowedTypes = arrayListOf(
                        TimeTypes.years,
                        TimeTypes.dates,
                        TimeTypes.nMonth,
                        TimeTypes.hours,
                        TimeTypes.minutes,
                        TimeTypes.seconds
                    )
                    parsedTimes.sortBy {
                        it.indexes[0]
                    }
                    var parsedTimesIndexes = findAllMatchingParsedTimes(
                        allowedTypes,
                        1,
                        index,
                        expressionSet.expressions,
                        parsedTimes
                    )
                    parsedTimesIndexes.sort();
                    if (parsedTimes.size > 0) {
                        var indexes = markIndexes(expressionSet, index, 1, true, false);
                        var foundTimeTypes = ArrayList<TimeTypes>()
                        for (i in parsedTimesIndexes) {
                            val index = parsedTimesIndexes[i];
                            var parsedTime = parsedTimes[index];
                            if (foundTimeTypes.contains(parsedTime.timeType)) {

                                parsedTimesIndexes.removeAt(i)
                                break;
                            } else {
                                indexes.addAll(parsedTime.indexes);
                                foundTimeTypes.add(parsedTime.timeType);
                            }
                        }
                        var i = parsedTimesIndexes.size;
                        while (i-- > 0) {
                            val index = parsedTimesIndexes[i];
                            var parsedTime = parsedTimes[index];
                            parsedTime.dateType = DateTypes.period_time;
                            parsedTime.indexes = indexes;
                            parsedTime.validMode = ValidModes.certified;
                            parsedTime.prevalence += prevalence;
                            if (parsedTime.timeType == TimeTypes.nMonth) {
                                parsedTime.timeType = TimeTypes.months;
                            }
                        }
                    }
                }
                return parsedTimes;
            })
    )

    val numberAndWordParseCases = arrayListOf(
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("ns")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt().toInt();
                    var max =
                        expressionSet.expressions[index + matches.group().length - 1].maximum.toInt()
                            .toInt();
                    var validMode = ValidModes.notCertified;
                    if (num > max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes = markIndexes(
                        expressionSet,
                        index,
                        matches.group().length,
                        true,
                        false
                    );
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.seconds,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)

                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("nm")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt();
                    var max =
                        expressionSet.expressions[index + matches.group().length - 1].maximum.toInt()
                            .toInt();
                    var validMode = ValidModes.notCertified;
                    if (num > max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.minutes,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)

                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("nh")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt();
                    var max =
                        expressionSet.expressions[index + matches.group().length - 1].maximum.toInt()
                            .toInt();
                    var validMode = ValidModes.notCertified;
                    if (num > max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, true);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.hours,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)

                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("n[dS]")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt();
                    var max =
                        expressionSet.expressions[index + matches.group().length - 1].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num > max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, true);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)

                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("nw")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt() * 7;
                    var max =
                        expressionSet.expressions[index + matches.group().length - 1].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num > max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, true);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)

                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("nQ{0,1}M")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt();
                    var max =
                        expressionSet.expressions[index + matches.group().length - 1].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num > max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, true);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)

                        )
                    );
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.months,
                            expressionSet.expressions[index + matches.group().length - 1].value.toInt(),
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)

                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("nK")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt();
                    if (0 < num) {
                        var indexes =
                            markIndexes(expressionSet, index, matches.group().length, true, true);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.nMonth,
                                num,
                                indexes,
                                context,
                                prevalence
                            )
                        );
                    }
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("[nN]y")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var num = expressionSet.expressions[index].text.toInt();
                    var max =
                        expressionSet.expressions[index + matches.group().length - 1].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num > max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.years,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("s")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.seconds,
                            1,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.notValid)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("m")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.minutes,
                            1,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.notValid)

                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("h")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.hours,
                            1,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.notValid)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("w")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            7,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.notValid)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("K")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.nMonth,
                            1,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.notValid)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("y")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.years,
                            1,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.notValid)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("d")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            1,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.notValid)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("0")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var text = expressionSet.expressions[index].text
                    var num = text.substring(0, text.length - 1).toInt();
                    var max = expressionSet.expressions[index].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num >= max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.seconds,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("1")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var text = expressionSet.expressions[index].text
                    var num = text.substring(0, text.length - 1).toInt();
                    var max = expressionSet.expressions[index].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num >= max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.minutes,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("2")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var text = expressionSet.expressions[index].text
                    var num = text.substring(0, text.length - 1).toInt();
                    var max = expressionSet.expressions[index].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num >= max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.hours,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("3")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var text = expressionSet.expressions[index].text
                    var num = text.substring(0, text.length - 1).toInt();
                    var max = expressionSet.expressions[index].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num >= max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("4")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var text = expressionSet.expressions[index].text
                    var num = text.substring(0, text.length - 1).toInt();
                    var max = expressionSet.expressions[index].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num >= max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.months,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)
                        )
                    );
                }
                return parsedTimes;
            }),
        ParseCase(numberAndWordPrevalence,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("5")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++
                    val index = matches.start()
                    var text = expressionSet.expressions[index].text
                    var num = text.substring(0, text.length - 1).toInt();
                    var max = expressionSet.expressions[index].maximum.toInt();
                    var validMode = ValidModes.notCertified;
                    if (num >= max && max != 0) {
                        validMode = ValidModes.notValid;
                    }
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, true, false);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.years,
                            num,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = validMode)
                        )
                    );
                }
                return parsedTimes;
            })
    )

    fun extractTime(expressions: ArrayList<Expression>, minimumPrevalence: Double): ExtractedTime {

        var parsedTimes = ArrayList<ParsedTime>()

        val expressionsSet = ExpressionSet("", expressions);
        for (expression in expressions) {
            expressionsSet.regchars += expression.regex_char;
        }
        val contextsData = ContextsData();
        val res = splitContext(expressions);
        contextsData.contexts = res.contexts;
        contextsData.usedContexts = arrayListOf()
        val separatingWords = res.separatingWords;
        for (parseCase in parseCases) {
            if (parseCase.prevalence >= minimumPrevalence) {

                parsedTimes = parseCase.parsefun(
                    expressionsSet,
                    parsedTimes,
                    contextsData,
                    parseCase.prevalence
                );
            }
        }
        if (numberAndWordPrevalence >= minimumPrevalence) {
            for (parseCase in numberAndWordParseCases) {
                parsedTimes = parseCase.parsefun(
                    expressionsSet,
                    parsedTimes,
                    contextsData,
                    parseCase.prevalence
                );
            }
        }
        for (parseCase in finalParseCases) {
            if (parseCase.prevalence >= minimumPrevalence) {
                parsedTimes = parseCase.parsefun(
                    expressionsSet,
                    parsedTimes,
                    contextsData,
                    parseCase.prevalence
                );
            }
        }
        return ExtractedTime(parsedTimes, contextsData, separatingWords)
    }

    data class ExtractedTime(
        val parsedTimes: ArrayList<ParsedTime>,
        val contextsData: ContextsData,
        val separatingWords: ArrayList<Int>
    )

    fun markIndexes(
        expressionSet: ExpressionSet,
        index: Int,
        end: Int,
        replacePrepositions: Boolean,
        allPrepositions: Boolean
    ): ArrayList<Int> {
        var this_index = index
        var this_end = end
        val prevChar = expressionSet.regchars.getOrNull(this_index - 1);
        if (replacePrepositions && (prevChar == 'p' || (allPrepositions && prevChar == 'P'))) {
            expressionSet.regchars = replaceAt(expressionSet.regchars, this_index - 1, '.');
            this_index--;
            this_end++;
        }
        var indexes = ArrayList<Int>()
        var i = maxOf(0, this_index);
        while (i < minOf(this_index + this_end, expressionSet.expressions.size)) {
            expressionSet.regchars = replaceAt(expressionSet.regchars, i, '.');
            indexes.add(i);
            i++
        }

        return indexes;
    }

    fun replaceAt(str: String, index: Int, char: Char): String {
        return str.substring(0, index) + char + str.substring(index + 1);
    }

    fun findCorrespondingContext(contextsData: ContextsData, indexes: ArrayList<Int>): Int {
        var i = indexes.size;
        val contexts = contextsData.contexts;
        while (i-- > 0) {
            var index = indexes[i];
            var j = contexts?.size!!;
            while (j-- > 0) {
                var start = contexts[j].start;
                var end = contexts[j].end;
                if (start <= index && index <= end) {
                    return j;
                }
            }
        }
        return -1;
    }

    fun processContexts(contextsData: ContextsData, indexes: ArrayList<Int>): Int {
        var context = findCorrespondingContext(contextsData, indexes);
        if (!contextsData.usedContexts?.contains(context)!!) {
            contextsData.usedContexts!!.add(context);
        }
        return context;
    }

    fun checkHours(value: Int): Boolean {
        return 0 <= value && value <= 24;
    }

    fun checkMinutes(value: Int): Boolean {
        return 0 <= value && value <= 59;
    }

    fun isLeapYear(year: Int): Boolean {
        return (year and 3) == 0 && ((year % 25) != 0 || (year and 15) == 0);
    }

    fun checkDate(date: Int, month: Int, year: Int): Boolean =
        runCatching { LocalDate(if (year == -1) reference.year else year, month, date) }.isSuccess

    fun checkMonth(month: Int): Boolean = month in 1..12

    fun checkYear(year: Int): Boolean {
        return 0 <= year;
    }

    fun processHour(hour: Int, partOfDay: Int): Int {
        if (0 < partOfDay) {
            if (partOfDay <= 4) {
                var l1 = (6 * partOfDay) % 24;
                var l2 = (6 * (partOfDay + 1)) % 24;
                if ((l1 < hour && hour <= l1 + 6) || (l2 <= hour && hour <= l2 + 6)) {
                    if (hour >= 12) {
                        return hour - 12;
                    } else {
                        return hour + 12;
                    }
                }
            } else if (partOfDay <= 6) {
                if (hour >= 12 && partOfDay == 5) {
                    return hour - 12;
                } else if (hour < 12 && partOfDay == 6) {
                    return hour + 12;
                }
            }
            return hour;
        }
        throw NumberFormatException()
    }

    class ValidModel(var validModes: ValidModes)

    val parseCases = arrayListOf(

        ParseCase(
            75,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("D")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    val index = matches.start()
                    var vals = expressionSet.expressions[index].text.split("\\.".toRegex());

                    var date = vals[0].toInt();
                    var month = vals[1].toInt().toInt();
                    var year = -1;
                    if (vals.size > 2) {
                        year = vals[2].toInt().toInt();
                    }
                    if (
                        checkDate(date, month, year)
                        && checkMonth(month)
                        && (year == -1 || checkYear(year))
                    ) {
                        var indexes = markIndexes(expressionSet, index, 1, true, true);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.dates,
                                date,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        )
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.months,
                                month,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)
                            )
                        );
                        if (year != -1) {
                            parsedTimes.add(
                                ParsedTime(
                                    DateTypes.target_date,
                                    TimeTypes.years,
                                    year,
                                    indexes,
                                    context,
                                    prevalence,
                                    OptionModel(validMode = ValidModes.certified)

                                )
                            );
                        }
                    } else if (checkHours(date) && checkMinutes(month) && vals.size == 2) {
                        expressionSet.regchars = replaceAt(expressionSet.regchars, index, 't');
                    }
                    i++;
                }

                return parsedTimes;
            }),

        ParseCase(75, fun(
            expressionSet: ExpressionSet,
            parsedTimes: ArrayList<ParsedTime>,
            contextsData: ContextsData,
            prevalence: Int
        ): ArrayList<ParsedTime> {
            val input = expressionSet.regchars

            val pattern = RulePattern.compile("t")
            val matches = pattern.matcher(input)

            var i = 0;
            while (matches.find()) {
                i++
                val index = matches.start()

                var vals = expressionSet.expressions[index].text.split("[.:]".toRegex());
                var hours = vals[0].toInt();
                var minutes = vals[1].toInt();
                var seconds = -1;
                if (vals.size > 2) {
                    if (matches.group()[2] == matches.group()[5] || matches.group()[1] == matches.group()[5]) {
                        seconds = vals[2].toInt();
                    } else {
                        hours = 100;
                    }
                }
                if (checkHours(hours) && checkMinutes(minutes) && (seconds == -1 || checkMinutes(
                        seconds
                    ))
                ) {
                    var indexes = markIndexes(expressionSet, index, 1, true, true);
                    var context = processContexts(contextsData, indexes);
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.hours,
                            hours,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(isFixed = true)
                        )
                    );
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.minutes,
                            minutes,
                            indexes,
                            context,
                            prevalence
                        )
                    );
                    if (seconds != -1) {
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.seconds,
                                seconds,
                                indexes,
                                context,
                                prevalence
                            )
                        );
                    }
                }
            }
            return parsedTimes;
        }),

        ParseCase(80,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("[FpP]d")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    val length = matches.group().length
                    var value = expressionSet.expressions[index + length - 1].value;
                    if (value != 0.0) {
                        var now = reference;
                        var dif = value - ((now.dayOfWeek.isoDayNumber % 7) + 1);
                        if (dif < 0) {
                            dif += 7;
                        }
                        if (matches.group()[0] == 'F' && dif == 0.0) {
                            dif += 7;
                        }
                        var indexes = markIndexes(expressionSet, index, length, true, true);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.dates,
                                now.day + dif.toInt(),
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)
                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("[pP]BnnO{0,1}")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    val length = matches.group().length
                    var minutes = expressionSet.expressions[index + 2].text.toInt()
                    var hours = expressionSet.expressions[index + 3].text.toInt()
                    if (1 <= hours && hours <= 25 && 0 < minutes && minutes < 60) {
                        var isFixed = false;
                        if (matches.group()[length - 1] == 'O') {
                            hours = processHour(
                                hours,
                                expressionSet.expressions[index + length - 1].value.toInt()
                            );
                            isFixed = true;
                        }
                        hours--;
                        minutes = 60 - minutes;
                        var indexes = markIndexes(expressionSet, index, length, false, false);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.hours,
                                hours,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(isFixed = isFixed, validMode = ValidModes.certified)

                            )
                        );
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.minutes,
                                minutes,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("Bnm{0,1}n[Oh]O{0,1}")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    val length = matches.group().length;
                    var minutes = expressionSet.expressions[index + 1].text.toInt();
                    var num = 0;
                    if (matches.group()[2] == 'm') {
                        num = expressionSet.expressions[index + 3].text.toInt();
                    } else {
                        num = expressionSet.expressions[index + 2].text.toInt();
                    }
                    if (1 <= num && num <= 25 && 0 < minutes && minutes < 60) {
                        var isFixed = false;
                        if (matches.group()[length - 1] == 'O') {
                            num = processHour(
                                num,
                                expressionSet.expressions[index + length - 1].value.toInt()
                            );
                            isFixed = true;
                        }
                        num--;
                        minutes = 60 - minutes;
                        var indexes = markIndexes(expressionSet, index, length, true, false);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.hours,
                                num,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(
                                    isFixed = isFixed, validMode = ValidModes.certified
                                )

                            )
                        );
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.minutes,
                                minutes,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(
                                    validMode = ValidModes.certified

                                )
                            ))
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("nm{0,1}Bn[Oh]O{0,1}")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    val length = matches.group().length;
                    var minutes = expressionSet.expressions[index].text.toInt().toInt();
                    var num = 0;
                    if (matches.group()[1] == 'm') {
                        num = expressionSet.expressions[index + 3].text.toInt();
                    } else {
                        num = expressionSet.expressions[index + 2].text.toInt();
                    }
                    if (1 <= num && num <= 25 && 0 < minutes && minutes < 60) {
                        var isFixed = false;
                        if (matches.group()[length - 1] == 'O') {
                            num = processHour(
                                num,
                                expressionSet.expressions[index + length - 1].value.toInt()
                            );
                            isFixed = true;
                        }
                        num--;
                        minutes = 60 - minutes;
                        var indexes = markIndexes(expressionSet, index, length, true, false);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.hours,
                                num,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(isFixed = isFixed, validMode = ValidModes.certified)

                            )
                        );
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.minutes,
                                minutes,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("HnO{0,1}")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    var num = expressionSet.expressions[index + 1].text.toInt() - 1;
                    if (0 <= num && num <= 23) {
                        var isFixed = false;
                        if (matches.group()[matches.group().length - 1] == 'O') {

                            num = processHour(
                                num,
                                expressionSet.expressions[index + 2].value.toInt()
                            );
                            isFixed = true;
                        }
                        var indexes =
                            markIndexes(expressionSet, index, matches.group().length, true, true);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.hours,
                                num,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(isFixed, validMode = ValidModes.certified)

                            )
                        );
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.minutes,
                                30,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("HL{0,1}n(O{1}|h{0,1})")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    var offset = 0;
                    if (matches.group()[1] == 'L') {
                        offset = 1;
                    }
                    var num = expressionSet.expressions[index + offset + 1].text.toInt();
                    val min = if (offset == 1) 0 else 1
                    val max = if (offset == 1) 23 else 24
                    if (min <= num && num <= max) {
                        var isFixed = false;
                        if (matches.group()[matches.group().length - 1] == 'O') {
                            num = processHour(
                                num,
                                expressionSet.expressions[index + offset + 2].value.toInt()
                            );
                            isFixed = true;
                        }
                        if (offset == 0) {
                            num--;
                        }
                        var indexes =
                            markIndexes(expressionSet, index, matches.group().length, true, true);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.hours,
                                num,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(isFixed, validMode = ValidModes.certified)

                            )
                        );
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.minutes,
                                30,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(75,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("nO")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    var value = expressionSet.expressions[index + 1].value.toInt();
                    var num = expressionSet.expressions[index].text.toInt().toInt();
                    if (checkHours(num)) {
                        num = processHour(num, value);
                        var indexes =
                            markIndexes(expressionSet, index, matches.group().length, true, true);
                        var context = processContexts(contextsData, indexes);
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.hours,
                                num,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(isFixed = true, validMode = ValidModes.certified)

                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("CA")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    var value = expressionSet.expressions[index + 1].value.toInt() + 1;
                    var indexes = markIndexes(expressionSet, index, 2, true, true);
                    var context = processContexts(contextsData, indexes);
                    val now = reference;
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            now.day + value,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.certified)

                        )
                    );
                }
                return parsedTimes;
            }),

        ParseCase(90,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("CX")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    var indexes =
                        markIndexes(expressionSet, index, matches.group().length, false, true);
                    var context = processContexts(contextsData, indexes);
                    val now = reference;
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.hours,
                            now.hour,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(isOffset = true, validMode = ValidModes.certified)

                        )
                    );
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.minutes,
                            now.minute + 30,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(isOffset = true, validMode = ValidModes.certified)

                        )
                    );

                    val notFoundTimeTypes = arrayListOf<TimeTypes>(
                        TimeTypes.dates,
                        TimeTypes.months,
                        TimeTypes.seconds,
                        TimeTypes.years
                    );
                    for (timeType in notFoundTimeTypes) {
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                timeType,
                                getDateProperty(now, timeType, true),
                                indexes,
                                context,
                                prevalence,
                                OptionModel(isOffset = true, validMode = ValidModes.certified)

                            )
                        );
                    }
                }
                return parsedTimes;
            }),

        ParseCase(60,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("A")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    var value = expressionSet.expressions[index].value.toInt();
                    var indexes = markIndexes(expressionSet, index, 1, true, true);
                    var context = processContexts(contextsData, indexes);
                    val now = reference;
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.dates,
                            now.day + value,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.certified)

                        )
                    );
                    parsedTimes.add(
                        ParsedTime(
                            DateTypes.target_date,
                            TimeTypes.months,
                            (now.month.number - 1) + 1,
                            indexes,
                            context,
                            prevalence,
                            OptionModel(validMode = ValidModes.certified)

                        )
                    );
                }
                return parsedTimes;
            }),

        ParseCase(60,
            fun(
                expressionSet: ExpressionSet,
                parsedTimes: ArrayList<ParsedTime>,
                contextsData: ContextsData,
                prevalence: Int
            ): ArrayList<ParsedTime> {
                val input = expressionSet.regchars
                val pattern = RulePattern.compile("Ed")
                val matches = pattern.matcher(input)

                var i = 0;
                while (matches.find()) {
                    i++

                    val index = matches.start()
                    var value = expressionSet.expressions[index + 1].value.toInt();
                    var indexes = markIndexes(expressionSet, index, 2, true, false);
                    var context = processContexts(contextsData, indexes);
                    val now = reference;
                    if (value == 0) {
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.period_time,
                                TimeTypes.dates,
                                1,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        );
                    } else {
                        var dif = value - ((now.dayOfWeek.isoDayNumber % 7) + 1);
                        if (dif < 0) {
                            dif += 7;
                        }
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.period_time,
                                TimeTypes.dates,
                                7,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        );
                        parsedTimes.add(
                            ParsedTime(
                                DateTypes.target_date,
                                TimeTypes.dates,
                                now.day + dif,
                                indexes,
                                context,
                                prevalence,
                                OptionModel(validMode = ValidModes.certified)

                            )
                        );
                    }
                }
                return parsedTimes;
            })
    )

    fun findParsedTimesByIndex(parsedTimes: ArrayList<ParsedTime>, index: Int): ArrayList<Int> {
        var i = parsedTimes.size;
        val result = ArrayList<Int>();
        while (i-- > 0) {
            if (parsedTimes[i].indexes.contains(index)) {
                result.add(i);
            }
        }
        return result;
    }

    fun findAllMatchingParsedTimes(
        allowedTypes: ArrayList<TimeTypes>,
        step: Int,
        start: Int,
        expressions: ArrayList<Expression>,
        parsedTimes: ArrayList<ParsedTime>,
        _ignoreValidMode: ValidModes? = null
    ): ArrayList<Int> {
        var ignoreValidMode = _ignoreValidMode
        if (ignoreValidMode == null) {
            ignoreValidMode = ValidModes.certified;
        }
        var i = start + step;
        var proceed = true;
        var result = ArrayList<Int>();
        while (proceed && (0 <= i && i < expressions.size)) {
            var indexes = findParsedTimesByIndex(parsedTimes, i);
            if (indexes.size > 0) {
                var newIndexes = ArrayList<Int>();
                for (index in indexes) {
                    val parsedTime = parsedTimes[index]
                    if (allowedTypes.contains(parsedTime.timeType) && parsedTime.validMode != ignoreValidMode) {
                        if (!result.contains(index)) {
                            newIndexes.add(index);
                        }
                    } else {
                        proceed = false;
                        newIndexes = ArrayList<Int>();
                        break;
                    }
                }
                if (newIndexes.size > 0) {
                    result.addAll(newIndexes);
                }
            } else {
                proceed = false;
            }
            i += step;
        }
        return result;
    }
}

internal data class ExpressionSet(var regchars: String, var expressions: ArrayList<Expression>)
internal data class SeparatingWords(
    var separatingWords: ArrayList<Int>,
    var contexts: ArrayList<Context>
)

internal enum class ValidModes {
    certified,
    notCertified,
    notValid,
    none
}

internal data class OptionModel(
    val isOffset: Boolean = false,
    val isFixed: Boolean = false,
    val validMode: ValidModes? = null
)

internal data class ParsedTime(
    var dateType: DateTypes,
    var timeType: TimeTypes,
    var number: Int,
    var indexes: ArrayList<Int>,
    val context: Int,
    var prevalence: Int,
    val options: OptionModel? = null
) {

    lateinit var validMode: ValidModes;

    var isOffset: Boolean = false

    var isFixed: Boolean = false

    init {

        if (options != null) {
            this.validMode =
                if (options.validMode == null) ValidModes.notCertified else options.validMode;
            this.isOffset = if (options.isOffset == null) false else options.isOffset;
            this.isFixed = if (options.isFixed == null) false else options.isFixed;
        } else {
            this.validMode = ValidModes.notCertified;
            this.isOffset = false
            this.isFixed = false
        }
    }
}

internal class ParseCase(
    var prevalence: Int, var parsefun:
        (expressionsSet: ExpressionSet, parsedTimes: ArrayList<ParsedTime>, contextsData: ContextsData, prevalence: Int) -> ArrayList<ParsedTime>
) {

}

internal class Context(val start: Int, val end: Int) {

}

internal class ContextsData() {

    var contexts: ArrayList<Context>? = null

    var usedContexts: ArrayList<Int>? = null
    val relativePeriods = mutableMapOf<Int, DateTimePeriod>()

}


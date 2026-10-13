package io.github.alox1d.dateparser.internal

internal class ContextHandling {
    object ContextHandling {
        
        fun findParsedDateByContext(contextIndex:Int, parsedDates:ArrayList<ParsedDate>):Int {
            var i = parsedDates.size;
            while (i-- > 0) {
                val parsedDate = parsedDates[i];
                if (parsedDate.contexts.contains(contextIndex)) {
                    return i;
                }
            }
            return -1;
        }

        fun addWordsFromContext(
            parsedDates: ArrayList<ParsedDate>,
            parsedDateIndex: Int,
            index: Int,
            contextsData: ContextsData,
            expressions: ArrayList<Expression?>,
            atEnd: Boolean,
            regexChar: String?,
            endChar: String?
        ):Boolean {
            var result = false;
            var lastChar:String? = null;
            var lastExpression:Expression? = null;
            if (atEnd) {
                lastExpression = expressions[contextsData.contexts?.get(index - 1)!!.end];
                lastChar = lastExpression!!.text;
            } else {
                lastExpression = expressions[contextsData.contexts?.get(index)!!.end];
                lastChar = lastExpression!!.text;
            }
            lastChar = lastChar[lastChar.length - 1].toString();
            if (regexChar != null) {
                if (lastExpression.regex_char == regexChar) {
                    val words = ArrayList<Expression>();
                    val context = contextsData.contexts!![index];
                    for (i in context.start..context.end) {
                        words.add(expressions[i]!!);
                    }
                    if (atEnd) {
                        (parsedDates[parsedDateIndex].string as ArrayList<Expression?>).addAll(words);
                    } else {

                        (parsedDates[parsedDateIndex].string as ArrayList<Expression?>).addAll(0,words);

                    }
                    result = true;
                }
            } else if (endChar != null) {
                if (lastChar == endChar) {
                    val words = ArrayList<Expression>();
                    val context = contextsData.contexts!![index];
                    for ( i in context.start..context.end) {
                        words.add(expressions[i]!!);
                    }
                    if (atEnd) {
                        (parsedDates[parsedDateIndex].string as ArrayList<Expression?>).addAll(words);
                    } else {
                        if (endChar == ".") {
                            (parsedDates[parsedDateIndex].string as ArrayList<Expression?>).addAll(0,words);
                            val nextWordText = (parsedDates[parsedDateIndex].string as ArrayList<Expression?>)[1]!!.text;
                            (parsedDates[parsedDateIndex].string as ArrayList<Expression?>)[1]!!.text = nextWordText[0].uppercaseChar() + nextWordText.substring(1);
                        } else {
                            (parsedDates[parsedDateIndex].string as ArrayList<Expression?>).addAll(0,words);
                        }
                    }
                    result = true;
                }
            }
            if (result) {
                parsedDates[parsedDateIndex].contexts.add(index);
            }
            return result;
        }

        fun fillNotUsedContexts(parsedDates:ArrayList<ParsedDate>, notUsedContexts:ArrayList<Int>, contextsData:ContextsData,
                                expressions:ArrayList<Expression?>, regexChar:String?, endChar:String?):ArrayList<Int> {
            val result = ArrayList<Int>();
            for ( index in notUsedContexts) {
                var proceed = true;
                if (index > 0) {
                    val parsedDateIndex = findParsedDateByContext(index - 1, parsedDates);
                    if (parsedDateIndex != -1) {
                        if (addWordsFromContext(parsedDates ,parsedDateIndex, index, contextsData, expressions, true, regexChar, endChar)) {
                            proceed = false;
                        }
                    }
                }
                if (proceed && index < contextsData.contexts!!.size - 1) {
                    val parsedDateIndex = findParsedDateByContext(index + 1, parsedDates);
                    if (parsedDateIndex != -1) {
                        if (addWordsFromContext(parsedDates, parsedDateIndex, index, contextsData, expressions, false, regexChar, endChar)) {
                            proceed = false;
                        }
                    }
                }
                if (proceed) {
                    result.add(index);
                }
            }
            return result;
        }

        fun proceedNotUsedContexts(parsedDates: ArrayList<ParsedDate>, separators:ParsingRules, _notUsedContexts:ArrayList<Int>,
                                   contextsData:ContextsData, expressions:ArrayList<Expression?>):ArrayList<Int> {
            var notUsedContexts = _notUsedContexts
            var newContexts = fillNotUsedContexts(parsedDates, notUsedContexts, contextsData, expressions, "I", null);
            if (newContexts.size != notUsedContexts.size) {
                newContexts = proceedNotUsedContexts(parsedDates, separators, newContexts, contextsData, expressions);
            }
            notUsedContexts = newContexts;
            for ( separator in separators.expressions) {
                newContexts = fillNotUsedContexts(parsedDates, newContexts, contextsData, expressions, null,
                    separator.text[separator.text.length - 1].toString()
                );
                if (newContexts.size != notUsedContexts.size   ) {
                    newContexts = proceedNotUsedContexts(parsedDates, separators, newContexts, contextsData, expressions);
                }
                notUsedContexts = newContexts;
            }
            return notUsedContexts;
        }
    }
}
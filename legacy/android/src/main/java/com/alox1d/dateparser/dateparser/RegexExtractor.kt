package ru.alox1d.wordnote.huawei.domain.services.dateparser

import android.content.Context
import android.util.Log
import java.util.*

class RegexExtractor {
    /**
     * Converts string to array of Expressions according to rules in ./expressions/ *.csv files
     * @param {String} string
     * @param {Number} errorLimit
     * @returns {Array.<Expression>} array of Expressions
     */
companion object {
    fun extractRegexChars(context: Context, string: String, errorLimit: Double = 1.0):ArrayList<Expression?> {
//        if (typeof (errorLimit) == 'undefined') {
//            errorLimit = 1;
//        }
        val words: List<String> = string.split(' ');
        val expressions = arrayListOf<Expression?>()
        expressions.addAll(Collections.nCopies(words.size,null))
        val parsingRules = Loader.getInstance(context).LoadExpressions()
        var k = words.size
        while (k-- > 0) {
            var word: String = words[k].lowercase(Locale.ROOT);
            val pattern = "[,.;:!?]".toRegex()
//            val match = word.matches(pattern)
            val match = pattern.find(word)
            if (match != null) {
                val m = match.value
                val idx = match.range
                if (idx.start + m.length == word.length)
                    word = word.substring(0, idx.start);
//                println("'$m' found at indexes $idx in '$str'")
            }
//            if (match && match.index + match[0].length == word.length) {
//                word = word.substring(0, match.index);
//            }
            var found = false;
            var i = parsingRules.size;
            var expression:Expression?=null;
            while (i-->0 && !found) {
                val parsingRule = parsingRules[i];
                var j = parsingRule.expressions.size;
                val expressionsLocal = parsingRule.expressions;
                while (j-->0 && !found) {
                    expression = expressionsLocal[j].copy()
                    if (expression.is_regex) {
                        val regexp = expression.text.toRegex()

//                        val match = words[k].match(regexp)
                        val matchj = regexp.find(words[k])

                        if (matchj != null && matchj.groupValues.size >= 1) {
                            found = true;
                            expression.text = words[k];
                        } else {
                            Log.d("TAG", "debug")
                        }
                    } else if (distance(expression.text, word) <= expression.errors_limit * errorLimit) {
                        found = true;
                        expression.text = words[k];
                    }
//                    j--
                }
//                i--
            }
            if (!found) {
                expression = Expression(words[k], ".", 0.0, 0.0, 0.0)
            }
            expressions.set(k,expression);
        }
        return expressions;
    }}
}
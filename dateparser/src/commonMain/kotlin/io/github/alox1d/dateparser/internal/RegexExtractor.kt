package io.github.alox1d.dateparser.internal

internal object RegexExtractor {
    private val unitWords by lazy {
        Loader.LoadExpressions().flatMap { it.expressions }
            .filter { !it.is_regex && it.value == 0.0 && it.regex_char in setOf("s", "m", "h", "d", "w", "K", "y") }
            .map { it.text.lowercase() }.toSet()
    }
    private fun matchingText(text: String): String = text.trim { it in ",.;!?()" }
    fun parsingText(expression: Expression): String {
        if (!expression.is_regex) return expression.text
        val value = matchingText(expression.text)
        val separator = expression.text.lastOrNull()?.takeIf { it == ',' || it == '.' }
        return value + (separator?.toString() ?: "")
    }
    fun extractRegexChars(string: String, errorLimit: Double): ArrayList<Expression?> {
        val words = string.trim().split(Regex("\\s+"))
        val expressions = arrayListOf<Expression?>()
        var inPeriodContext = false
        for ((wordIndex, original) in words.withIndex()) {
            val token = matchingText(original)
            val word = token.lowercase()
            // "2 дня" is a day count; "5 часов дня" retains the afternoon rule.
            if (word == "дня" && wordIndex > 0 && matchingText(words[wordIndex - 1]).toIntOrNull() != null) {
                expressions.add(Expression(original, "d", 0.0, 0.0, 0.0))
                continue
            }
            val periodCount = inPeriodContext && wordIndex + 1 < words.size &&
                matchingText(words[wordIndex + 1]).lowercase() in unitWords
            var found: Expression? = null
            for (cluster in Loader.LoadExpressions().asReversed()) {
                for (rule in cluster.expressions.asReversed()) {
                    if (rule.regex_char == "N" && periodCount) continue
                    val matches = if (rule.is_regex) rule.regex!!.matches(token) ||
                        (rule.regex_char in listOf("n", "N") && token.startsWith("-") && rule.regex.matches(token.drop(1)))
                        else distance(rule.text, word) <= rule.errors_limit * errorLimit
                    if (matches) { found = rule.copy(text = original); break }
                }
                if (found != null) break
            }
            expressions.add(found ?: Expression(original, ".", 0.0, 0.0, 0.0))
            if (found?.regex_char in setOf("C", "E")) inPeriodContext = true
            if (found?.regex_char == "I" || word in setOf("or", "then") || original.lastOrNull() in setOf(',', ';')) {
                inPeriodContext = false
            }
        }
        return expressions
    }
}

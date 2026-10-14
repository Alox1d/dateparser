package io.github.alox1d.dateparser.internal

internal object RegexExtractor {
    private val punctuation = Regex("[,.;:!?]")
    fun extractRegexChars(string: String, errorLimit: Double): ArrayList<Expression?> {
        val words = string.trim().split(Regex("\\s+"))
        val expressions = arrayListOf<Expression?>()
        for ((wordIndex, original) in words.withIndex()) {
            var word = original.lowercase()
            val suffix = punctuation.find(word)
            if (suffix != null && suffix.range.last == word.lastIndex) word = word.dropLast(1)
            // "2 дня" is a day count; "5 часов дня" retains the afternoon rule.
            if (word == "дня" && wordIndex > 0 && words[wordIndex - 1].toIntOrNull() != null) {
                expressions.add(Expression(original, "d", 0.0, 0.0, 0.0))
                continue
            }
            var found: Expression? = null
            for (cluster in Loader.LoadExpressions().asReversed()) {
                for (rule in cluster.expressions.asReversed()) {
                    val matches = if (rule.is_regex) rule.regex!!.containsMatchIn(original)
                        else distance(rule.text, word) <= rule.errors_limit * errorLimit
                    if (matches) { found = rule.copy(text = original); break }
                }
                if (found != null) break
            }
            expressions.add(found ?: Expression(original, ".", 0.0, 0.0, 0.0))
        }
        return expressions
    }
}

package io.github.alox1d.dateparser.internal

internal data class Expression(
    var text: String, val regex_char: String, val errors_limit: Double,
    val value: Double, val maximum: Double, val is_regex: Boolean = false,
    var is_separator: Boolean = false
) {
    var context: Int = 0
    val regex: Regex? = if (is_regex) Regex(text) else null
}
internal class ParsingRules(val expressions: List<Expression>)
internal object Loader {
    private val expressions = listOf(ParsingRules(BundledRules.en), ParsingRules(BundledRules.ru), ParsingRules(BundledRules.regexs))
    private val separators = ParsingRules(BundledRules.separators)
    fun LoadExpressions(): List<ParsingRules> = expressions
    fun LoadSeparators(): ParsingRules = separators
}

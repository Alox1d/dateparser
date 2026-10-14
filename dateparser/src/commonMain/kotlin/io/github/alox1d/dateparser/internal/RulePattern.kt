package io.github.alox1d.dateparser.internal

/** Small common Regex cursor used by the original rule algorithm. */
internal class RulePattern private constructor(private val regex: Regex) {
    fun matcher(input: String): RuleMatcher = RuleMatcher(regex, input)
    companion object { fun compile(pattern: String): RulePattern = RulePattern(Regex(pattern)) }
}
internal class RuleMatcher(private val regex: Regex, private val input: String) {
    private val matches = regex.findAll(input).iterator()
    private var current: MatchResult? = null
    fun find(): Boolean {
        if (!matches.hasNext()) return false
        current = matches.next()
        return true
    }
    fun start(): Int = checkNotNull(current).range.first
    fun group(): String = checkNotNull(current).value
    fun length(): Int = regex.findAll(input).count()
    fun findByIndex(index: Int = 0): Int {
        var found = 0
        while (find()) {
            if (found++ == index) return start()
        }
        return -1
    }
}

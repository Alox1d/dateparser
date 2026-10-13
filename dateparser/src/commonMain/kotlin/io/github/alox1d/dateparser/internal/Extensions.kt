package io.github.alox1d.dateparser.internal
internal fun String.padLeft(n: Int, s: String?): String = (s ?: "null").padStart(n)
internal fun Boolean.toInt(): Int = if (this) 1 else 0

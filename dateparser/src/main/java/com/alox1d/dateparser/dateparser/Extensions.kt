package ru.alox1d.wordnote.huawei.domain.services.dateparser

fun String.padRight(n: Int, s: String?): String? {
    return String.format("%-" + n + "s", s)
}

fun String.padLeft(n: Int, s: String?): String? {
    return String.format("%" + n + "s", s)
}

fun Boolean.toInt() = if (this) 1 else 0

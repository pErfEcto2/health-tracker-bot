package com.trackhub.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** ISO yyyy-MM-dd in LOCAL timezone. Mirrors the web client fix. */
object DateUtils {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(ISO)

    fun daysAgo(days: Int): String = LocalDate.now().minusDays(days.toLong()).format(ISO)

    fun shift(iso: String, deltaDays: Int): String {
        return try {
            LocalDate.parse(iso, ISO).plusDays(deltaDays.toLong()).format(ISO)
        } catch (_: Exception) { iso }
    }

    /** For display, e.g. "13 апр". */
    fun displayShort(iso: String): String = try {
        val d = LocalDate.parse(iso, ISO)
        "${d.dayOfMonth} ${monthShortRu(d.monthValue)}"
    } catch (_: Exception) { iso }

    private fun monthShortRu(m: Int): String = when (m) {
        1 -> "янв"; 2 -> "фев"; 3 -> "мар"; 4 -> "апр"; 5 -> "мая"; 6 -> "июн"
        7 -> "июл"; 8 -> "авг"; 9 -> "сен"; 10 -> "окт"; 11 -> "ноя"; 12 -> "дек"
        else -> ""
    }
}

package ru.pavlig43.core

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant




private val dateTimeFormat = LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}
@OptIn(ExperimentalTime::class)
fun Long.convertToDateTime(
    ): String {
    val timezone: TimeZone = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(timezone)
    return dateTimeFormat.format(localDateTime)
}
private val dateFormat = LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()

}
@OptIn(ExperimentalTime::class)
fun Long.convertToDate(
): String {
    val timezone: TimeZone = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(timezone)
    return dateFormat.format(localDateTime)
}

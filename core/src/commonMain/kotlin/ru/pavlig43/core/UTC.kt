package ru.pavlig43.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime


private val dateTimeFormat = LocalDateTime.Format {
    dayOfMonth()
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
fun Long.convertToDateTime(
    ): String {
    val timezone: TimeZone = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(timezone)
    return dateTimeFormat.format(localDateTime)
}
private val dateFormat = LocalDateTime.Format {
    dayOfMonth()
    char('.')
    monthNumber()
    char('.')
    year()

}
fun Long.convertToDate(
): String {
    val timezone: TimeZone = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(timezone)
    return dateFormat.format(localDateTime)
}

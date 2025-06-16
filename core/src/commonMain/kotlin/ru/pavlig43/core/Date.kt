package ru.pavlig43.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime


@JvmInline
value class UTC(val value: Long)

private val dateFormat = LocalDateTime.Format {
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
fun UTC.convertToDateTime(
    ): String {
    val timezone: TimeZone = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(this.value)
    val localDateTime = instant.toLocalDateTime(timezone)
    return dateFormat.format(localDateTime)
}


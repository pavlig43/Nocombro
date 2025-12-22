package ru.pavlig43.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
fun getCurrentLocalDate(): LocalDate {
    return getCurrentLocalDateTime().date
}
@OptIn(ExperimentalTime::class)
fun getCurrentLocalDateTime(): LocalDateTime {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
}




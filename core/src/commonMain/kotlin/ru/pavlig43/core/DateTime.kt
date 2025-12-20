package ru.pavlig43.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


sealed interface DateFieldKind {
    data object Date : DateFieldKind
    data object DateTime : DateFieldKind
}

@OptIn(ExperimentalTime::class)
fun Long.convertToDateOrDateTimeString(kind: DateFieldKind): String {
    val timezone = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(timezone)
    return when (kind) {
        DateFieldKind.Date -> dateFormat.format(localDateTime)
        DateFieldKind.DateTime -> dateTimeFormat.format(localDateTime)
    }
}

@OptIn(ExperimentalTime::class)
fun getUTCNow(): Long {
    return Clock.System.now().toEpochMilliseconds()
}
@OptIn(ExperimentalTime::class)
fun getCurrentLocalDate(): LocalDate {
    return getCurrentLocalDateTime().date
}
@OptIn(ExperimentalTime::class)
fun getCurrentLocalDateTime(): LocalDateTime {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
}

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
}


private val dateFormat = LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()

}

@OptIn(ExperimentalTime::class)
object UTCConverter {

    fun toLocalDateTime(utc: Long): LocalDateTime {
        return Instant.fromEpochMilliseconds(utc)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }


}

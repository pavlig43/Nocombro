package ru.pavlig43.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface DateThreshold {
    val value: LocalDate
    val displayName: String

    object OneMonth : DateThreshold {
        override val value: LocalDate = getCurrentLocalDate().plus(1, DateTimeUnit.MONTH)
        override val displayName: String = "1 Месяц"
    }

    object ThreeMonth : DateThreshold {
        override val value: LocalDate = getCurrentLocalDate().plus(3, DateTimeUnit.MONTH)
        override val displayName: String = "3 Месяца"

    }

    object Now : DateThreshold {
        override val value: LocalDate = getCurrentLocalDate()
        override val displayName: String = "Сегодня"
    }
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

val dateTimeFormat = LocalDateTime.Format {
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


val dateFormat = LocalDate.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()

}
val emptyDate = LocalDate(1900, 1, 1)




package ru.pavlig43.core

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Пороговые значения дат для фильтрации и отображения временных интервалов.
 *
 * Каждый объект представляет конкретную дату относительно текущего момента
 * с человеко-читаемым названием для отображения в UI.
 *
 * @property value Конкретная дата порога
 * @property displayName Название для отображения в интерфейсе
 */
sealed interface DateThreshold {
    val value: LocalDate
    val displayName: String

    object OneMonth : DateThreshold {
        override val value: LocalDate = getCurrentLocalDate().plus(1, DateTimeUnit.MONTH)
        override val displayName: String = "1 Месяц"
    }

    @Suppress("MagicNumber")
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
@Suppress("MagicNumber")
val emptyDate = LocalDate(1900, 1, 1)

@Suppress("MagicNumber")
val emptyLocalDateTime = LocalDateTime(1900, 1, 1, 0, 0)

class DateComponent(
    componentContext: ComponentContext,
    initDate: LocalDate,
    val onChangeDate: (LocalDate) -> Unit,
    val onDismissRequest:()-> Unit
) : ComponentContext by componentContext {
    private val _date = MutableStateFlow(initDate)
    val date = _date.asStateFlow()

}
class DateTimeComponent(
    componentContext: ComponentContext,
    initDatetime: LocalDateTime,
    val onChangeDate: (LocalDateTime) -> Unit,
    val onDismissRequest:()-> Unit
) : ComponentContext by componentContext {
    private val _dateTime = MutableStateFlow(initDatetime)
    val dateTime = _dateTime.asStateFlow()

}




@file:Suppress("TooManyFunctions","MagicNumber")
package ru.pavlig43.core

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
/**
 * Значение дня недели по стандарту ISO8601, от 1 (понедельник) до 7 (воскресенье).
 */
public val DayOfWeek.value: Int get() = ordinal + 1

/**
 * Возвращает новый LocalDateTime с тем же (если [this] тот же день недели, что и [dayOfWeek]) или
 * предыдущим [dayOfWeek].
 */
public fun LocalDateTime.withPreviousOrSameDayOfWeek(
    dayOfWeek: DayOfWeek,
    zone: TimeZone,
): LocalDateTime {
    val daysDiff = dayOfWeek.value - this.dayOfWeek.value
    if (daysDiff == 0) return this

    val daysToAdd = if (daysDiff >= 0) 7 - daysDiff else -daysDiff
    return minusDays(daysToAdd, zone)
}

/**
 * Возвращает новый LocalDateTime, установленный на [DayOfWeek.MONDAY] и [00:00:00.000] недели [this].
 */
public fun LocalDateTime.asStartOfWeek(zone: TimeZone): LocalDateTime =
    withPreviousOrSameDayOfWeek(DayOfWeek.MONDAY, zone).asMidnight()

/**
 * Возвращает новый LocalDateTime, установленный на первый день месяца и [00:00:00.000] месяца [this].
 */
public fun LocalDateTime.asStartOfMonth(): LocalDateTime = LocalDateTime(year, month, 1, 0, 0, 0, 0)

/**
 * Возвращает новый LocalDateTime, установленный на первый день года и [00:00:00.000] года [this].
 */
public fun LocalDateTime.asStartOfYear(): LocalDateTime = LocalDateTime(year, Month.JANUARY, 1, 0, 0, 0, 0)

/**
 * Добавляет заданное значение к [this], преобразуя его в [Instant] для добавления.
 */
public fun LocalDateTime.plus(value: Int, unit: DateTimeUnit, zone: TimeZone): LocalDateTime =
    toInstant(zone).plus(value, unit, zone).toLocalDateTime(zone)

/**
 * Возвращает новый [LocalDateTime] с заданным [day] днём месяца и остальными данными из [this].
 */
public fun LocalDateTime.withDayOfMonth(day: Int): LocalDateTime =
    LocalDateTime(year, month, day, hour, minute, second, nanosecond)

/**
 * Добавляет указанное количество [days] дней к [this].
 */
public fun LocalDateTime.plusDays(days: Int, zone: TimeZone): LocalDateTime = plus(days, DateTimeUnit.DAY, zone)

/**
 * Вычитает указанное количество [days] дней из [this].
 */
public fun LocalDateTime.minusDays(days: Int, zone: TimeZone): LocalDateTime = plus(-days, DateTimeUnit.DAY, zone)

/**
 * Возвращает новый [LocalDateTime] с заданным [month] месяцем и остальными данными из [this].
 */
public fun LocalDateTime.withMonth(month: Month): LocalDateTime =
    LocalDateTime(year, month, day, hour, minute, second, nanosecond)

/**
 * Добавляет указанное количество [months] месяцев к [this]. Возвращает новый [LocalDateTime]
 */
public fun LocalDateTime.plusMonths(months: Int, zone: TimeZone): LocalDateTime = plus(months, DateTimeUnit.MONTH, zone)

/**
 * Вычитает указанное количество [months] месяцев из [this]. Возвращает новый [LocalDateTime]
 */
public fun LocalDateTime.minusMonths(months: Int, zone: TimeZone): LocalDateTime =
    plus(-months, DateTimeUnit.MONTH, zone)

/**
 * Вычисляет длину текущего месяца [LocalDate] в днях
 */
public val LocalDate.lengthOfMonth: Int get() = month.length(year)

/**
 * Вычисляет длину текущего месяца [LocalDateTime] в днях
 */
public val LocalDateTime.lengthOfMonth: Int get() = month.length(year)

/**
 * Создаёт новый [Instant] и преобразует его в [LocalDateTime]
 */
public fun LocalDateTime.Companion.now(zone: TimeZone): LocalDateTime =
    Clock.System.now().toLocalDateTime(zone)

/**
 * Возвращает true, если [this] тот же день, что и [other]. Игнорирует время, но учитывает год и месяц.
 */
public fun LocalDateTime.onSameDay(other: LocalDateTime): Boolean = date == other.date

/**
 * Возвращает новый [LocalDateTime] с датой, скорректированной на следующую [dayOfWeek].
 * Если дата уже приходится на заданный [dayOfWeek], возвращает дату через неделю.
 */
public fun LocalDateTime.withNextDayOfWeek(
    dayOfWeek: DayOfWeek,
    zone: TimeZone,
): LocalDateTime {
    val daysDiff = this.dayOfWeek.value - dayOfWeek.value
    val daysToAdd = if (daysDiff >= 0) 7 - daysDiff else -daysDiff
    return plusDays(daysToAdd, zone)
}

/**
 * Возвращает новый [LocalDateTime], установленный на полночь.
 */
public fun LocalDateTime.asMidnight(): LocalDateTime = LocalDateTime(year, month, day, 0, 0, 0, 0)

/**
 * Возвращает Instant даты 1 января 1970 00:00:00.000 UTC.
 */
public val Instant.Companion.EPOCH: Instant
    get() = fromEpochMilliseconds(0)


/**
 * Определяет длину заданного месяца в зависимости от года, в днях.
 */
public fun Month.length(year: Int): Int {
    val start = LocalDate(year, this, 1)
    val end = start.plus(1, DateTimeUnit.MONTH)
    return start.until(end, DateTimeUnit.DAY).toInt()
}

/**
 * Получает текущую дату, используя указанную [zone].
 */
public fun LocalDate.Companion.now(zone: TimeZone): LocalDate = LocalDateTime.now(zone).date

/**
 * Возвращает новый LocalDate с днём месяца, установленным на указанный [day]
 */
public fun LocalDate.withDayOfMonth(day: Int): LocalDate = LocalDate(year, month, day)

/**
 * Возвращает новый LocalDate с добавленным указанным количеством [days] дней
 */
public fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)




# План: Добавить период от начала до конца текущего месяца

## Задача
Добавить в `DTPeriod` свойство для получения периода от начала до конца текущего месяца.

## Файл для изменения
`datetime/src/desktopMain/kotlin/ru/pavlig43/datetime/period/dateTime/DateTimePeriodComponent.kt`

## Изменения

### В companion object DTPeriod (строки 99-103)

Добавить новое свойство `thisMonth`:

```kotlin
val thisMonth: DTPeriod by lazy {
    val now = getCurrentLocalDateTime()
    val start = now.asStartOfMonth()
    val end = LocalDateTime(
        start.year,
        start.month,
        start.lengthOfMonth,
        23,
        59,
        59,
        999_999_999
    )
    DTPeriod(start, end)
}
```

## Пояснение
- `asStartOfMonth()` - уже есть в Utils.kt, возвращает первый день месяца в 00:00:00
- `lengthOfMonth` - уже есть в Utils.kt, возвращает количество дней в месяце
- Конец месяца: последний день месяца + время 23:59:59.999999999

## Использование
```kotlin
val period = DTPeriod.thisMonth
// start = 01.03.2026 00:00
// end = 31.03.2026 23:59
```

# План: Рефакторинг DecimalColumn.kt — объединение readDecimalColumn

## Цель
Устранить дублирование кода, объединив `readDecimalColumn` и `readDecimalColumnWithFooter` в одну функцию с опциональным параметром `footerValue`.

---

## Файл
`features/table/mutable/src/desktopMain/kotlin/ru/pavlig43/mutable/api/column/DecimalColumn.kt`

---

## Изменения

### 1. Обновить `readDecimalColumn` (строка 59-79)

**Добавить опциональный параметр `footerValue`:**

```kotlin
@Suppress("LongParameterList")
fun <T : Any, C, E, DECIMAL: DecimalData> EditableTableColumnsBuilder<T, C, E>.readDecimalColumn(
    key: C,
    getValue: (T) -> DECIMAL,
    headerText: String,
    footerValue: ((E) -> DECIMAL)? = null,
    filterType: TableFilterType.NumberTableFilter<DECIMAL>?= null,
    isSortable: Boolean = true
) {
    column(key, valueOf = { getValue(it) }) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }
        readNumberCell( getCount = { getValue(it) })
        footerValue?.let { accumulateFunction ->
            footer { tableData ->
                val accumValue = accumulateFunction(tableData)
                Text(
                    text = accumValue.toStartDoubleFormat(),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        if (isSortable) {
            sortable()
        }
    }
}
```

**Изменения:**
- Добавлен параметр `footerValue: ((E) -> DECIMAL)? = null`
- Раскомментирован `autoWidth(300.dp)` (для согласованности с `decimalColumn`)
- Добавлен блок футера (аналогично `decimalColumn:45-53`)

### 2. Удалить `readDecimalColumnWithFooter` (строка 81-108)

**Полностью удалить функцию.**

---

## Миграция вызовов

Найти все использования `readDecimalColumnWithFooter` и заменить на `readDecimalColumn`:

### Файл 1: `features/form/transaction/src/desktopMain/kotlin/ru/pavlig43/transaction/internal/update/tabs/component/buy/Column.kt`
- Заменить `readDecimalColumnWithFooter` на `readDecimalColumn` (имя параметра то же — `footerValue`)

### Файл 2: `features/form/transaction/src/desktopMain/kotlin/ru/pavlig43/transaction/internal/update/tabs/component/sale/Column.kt`
- Заменить `readDecimalColumnWithFooter` на `readDecimalColumn` (имя параметра то же — `footerValue`)

---

## Итоговая таблица изменений

| Строка | Функция | Действие | Описание |
|--------|---------|----------|----------|
| 59-79 | `readDecimalColumn` | Изменить | Добавить `footerValue` параметр |
| 68 | `readDecimalColumn` | Раскомментировать | `autoWidth(300.dp)` |
| 74-78 | `readDecimalColumn` | Добавить | Блок футера |
| 81-108 | `readDecimalColumnWithFooter` | **Удалить** | Вся функция |

---

## Проверка

1. Все вызовы `readDecimalColumnWithFooter` заменены на `readDecimalColumn`
2. Существующие вызовы `readDecimalColumn` (без футера) продолжают работать
3. API стал более консистентным: `decimalColumn` и `readDecimalColumn` оба имеют опциональный `footerValue`

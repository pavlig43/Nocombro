# План: Включение сортировки и фильтрации в Storage таблице

## Проблема

В таблице Storage (`features/storage/src/commonMain/kotlin/ru/pavlig43/storage/`) не работают сортировка и фильтрация:
1. **Основная таблица продуктов** - фильтры не настроены в колонках
2. **Вложенная таблица партий** - фильтры явно отключены (`autoApplyFilters = false`, `showActiveFiltersHeader = false`)

## Файлы для изменения

### 1. `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/column/Columns.kt`
**Проблема:** Колонки создаются без параметра `filterType`

**Изменения:** Добавить `filterType` во все вызовы `readTextColumn` и `readDecimalColumn`:

```kotlin
// Добавить импорт
import ua.wwind.table.filter.data.TableFilterType

// Текстовые колонки
readTextColumn(
    headerText = "Продукт",
    column = StorageProductField.PRODUCT_NAME,
    valueOf = { it.productName },
    filterType = TableFilterType.TextTableFilter()  // Добавить
)

// Числовые колонки
readDecimalColumn(
    headerText = "Старт",
    column = StorageProductField.BALANCE_BEFORE,
    valueOf = { it.balanceBeforeStart },
    decimalFormat = ru.pavlig43.immutable.internal.column.DecimalFormat.Decimal3(),
    filterType = TableFilterType.NumberTableFilter(
        delegate = TableFilterType.NumberTableFilter.IntDelegate
    )  // Добавить
)
```

### 2. `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/column/StorageBatchColumns.kt`
**Проблема:** Колонки партий создаются без `filterType`

**Изменения:** Добавить `filterType` во все колонки партий:

```kotlin
// Добавить импорт
import ua.wwind.table.filter.data.TableFilterType

readTextColumn(
    column = StorageBatchColumn.BATCH_NAME,
    valueOf = { it.batchName },
    headerText = "",
    filterType = TableFilterType.TextTableFilter()  // Добавить
)

readDecimalColumn(
    column = StorageBatchColumn.BALANCE_BEFORE,
    valueOf = { it.balanceBeforeStart },
    headerText = "Старт",
    decimalFormat = DecimalFormat.Decimal3(),
    filterType = TableFilterType.NumberTableFilter(
        delegate = TableFilterType.NumberTableFilter.IntDelegate
    )  // Добавить
)
// То же самое для INCOMING, OUTGOING, BALANCE_END
```

### 3. `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/ui/StorageBatchesSection.kt`
**Проблема:** Фильтры явно отключены в `TableSettings`

**Изменения:** Включить фильтры:

```kotlin
val batchSettings = remember {
    TableSettings(
        isDragEnabled = false,
        autoApplyFilters = false,        // Оставить как есть (ручное применение)
        showActiveFiltersHeader = true,  // ИЗМЕНИТЬ: было false → true
        stripedRows = true,
        rowHeightMode = RowHeightMode.Dynamic,
    )
}
```

### 4. `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/StorageScreen.kt`
**Проблема:** Не настроен `TableSettings` для основной таблицы

**Изменения:** Добавить `TableSettings` в `rememberTableState`:

```kotlin
// Добавить импорты
import ua.wwind.table.config.TableSettings

val tableSettings = remember {
    TableSettings(
        autoApplyFilters = false,
        showActiveFiltersHeader = true,
    )
}

val tableState = rememberTableState(
    columns = StorageProductField.entries.toImmutableList(),
    settings = tableSettings,  // Добавить
)
```

## Полный список фильтров для добавления

### Основная таблица (Columns.kt):
| Колонка | Тип фильтра |
|---------|------------|
| PRODUCT_NAME | `TableFilterType.TextTableFilter()` |
| BALANCE_BEFORE | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |
| INCOMING | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |
| OUTGOING | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |
| BALANCE_END | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |

### Вложенная таблица (StorageBatchColumns.kt):
| Колонка | Тип фильтра |
|---------|------------|
| BATCH_NAME | `TableFilterType.TextTableFilter()` |
| BALANCE_BEFORE | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |
| INCOMING | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |
| OUTGOING | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |
| BALANCE_END | `TableFilterType.NumberTableFilter(delegate = IntDelegate)` |

## Проверка

После внесения изменений:
1. Запустить Desktop приложение: `./gradlew :app:desktopApp:run`
2. Открыть экран "Склад"
3. Проверить:
   - Заголовок фильтров отображается
   - При клике на иконку фильтра появляется UI фильтрации
   - Фильтры работают для текстовых и числовых колонок
   - Сортировка работает при клике на заголовок колонки
   - Вложенная таблица партий также имеет работающие фильтры

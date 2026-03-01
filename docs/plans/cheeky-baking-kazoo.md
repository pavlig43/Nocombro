# План: Реализация expand/collapse для таблицы остатков на складах

## Цель
Добавить функционал разворачивания строк с партиями в таблице остатков (`ProductStorageComponent`) с подстроками, использующими те же колонки + отступ.

## Требования
- Подстроки используют те же колонки, что и основная строка (Старт, Приход, Расход, Остаток)
- **Ширина колонок подстрок совпадает с шириной колонок основной таблицы** (выравнивание по вертикали)
- Добавлен отступ слева для иерархии
- Колонка EXPAND у подстрок пустая
- Плавная анимация разворачивания/сворачивания

---

## Ссылки на готовые примеры

| Что | Файл |
|-----|-------|
| Mutable реализация | `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/ui/StorageTable.kt` |
| Mutable batches section | `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/ui/StorageBatchesSection.kt` |
| Sampletable пример | `features/sampletable/src/commonMain/kotlin/ru/pavlig43/sampletable/app/components/MainTable.kt` |

---

## Файлы для изменения

### 1. `features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/ProductStorageComponent.kt`
**Изменения:**
- Реализовать `onToggleExpand` callback
- Добавить метод `toggleExpand(productId: Int)` для изменения состояния `expanded`
- Обновить mapper чтобы respect сохранённый `expanded` state

### 2. `features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/Columns.kt`
**Изменения:**
- Добавить отступ (`padding(start = 24.dp)`) для первой колонки партий
- В колонке EXPAND для партий показывать пустую ячейку

### 3. `features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/ui/ImmutableListBox.kt`
**Изменения:**
- Добавить параметр `rowEmbedded` в функцию `ImmutableTable`
- Передать `rowEmbedded` в `Table` из библиотеки table-kmp

### 4. Создать: `features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/StorageBatchesSection.kt`
**Создать компонент для отображения партий:**
- Создать вложенную таблицу с колонками для партий
- Использовать `embedded = true` для стиля
- Добавить отступ через `Modifier.padding(start = 24.dp)`
- **Принимать `parentState: TableState<StorageProductField>` для синхронизации ширины колонок**

### 5. Создать: `features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/StorageBatchColumns.kt`
**Создать колонки для партий:**
- `BATCH_NAME` - название партии (с отступом)
- `BALANCE_BEFORE` - стартовый остаток
- `INCOMING` - приход
- `OUTGOING` - расход
- `BALANCE_END` - конечный остаток
- Колонка EXPAND - пустая для партий
- **Функция принимает `parentWidths: Map<StorageBatchColumn, Dp>` и применяет фиксированную ширину**

### 6. Создать: `features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/StorageBatchColumn.kt`
**Enum для колонок партий:**
```kotlin
enum class StorageBatchColumn {
    EXPAND,      // Пустая у партий
    BATCH_NAME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}
```

---

## Детали реализации

### Паттерн rowEmbedded (из sampletable)

```kotlin
rowEmbedded = { _, product ->
    val visible = product.expanded
    if (visible) {
        HorizontalDivider(
            thickness = state.dimensions.dividerThickness,
            modifier = Modifier.width(state.tableWidth),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        StorageBatchesSection(
            product = product,
            parentState = state  // ← Передаём state для синхронизации ширины
        )
    }
}
```

### Отступ для подстрок

Для отступа используем `Modifier.padding(start = 24.dp)` в:
1. `StorageBatchesSection` - обёртка Column
2. Или в первой колонке BATCH_NAME через cell modifier

### Управление состоянием expanded

```kotlin
// В ProductStorageComponent
private val _expandedProducts = mutableStateOf<Set<Int>>(emptySet())

fun toggleExpand(productId: Int) {
    _expandedProducts.update { current ->
        if (productId in current) current - productId
        else current + productId
    }
}

// В mapper
val expanded = productId in _expandedProducts.value
mapper = { toUi(expanded = expanded) }
```

### Синхронизация ширины колонок

**Проблема:** Вложенная таблица имеет свой `TableState` с независимыми ширинами колонок.

**Решение:** Передавать фиксированные ширину из родительского state:

```kotlin
@Composable
fun StorageBatchesSection(
    product: StorageProductUi,
    parentState: TableState<StorageProductField>,  // ← Добавить
    modifier: Modifier = Modifier,
) {
    val columns = remember {
        createStorageBatchColumns(
            parentWidths = mapOf(
                StorageBatchColumn.BALANCE_BEFORE to parentState.resolveColumnWidth(StorageProductField.BALANCE_BEFORE),
                StorageBatchColumn.INCOMING to parentState.resolveColumnWidth(StorageProductField.INCOMING),
                // ...
            )
        )
    }
    // ...
}
```

В `createStorageBatchColumns` использовать `.width()` для каждой колонки:

```kotlin
readDecimalColumn(
    key = StorageBatchColumn.BALANCE_BEFORE,
    getValue = { it.balanceBeforeStart },
    headerText = "Старт",
    decimalFormat = DecimalFormat.Decimal3(),
    isSortable = false
).width(widths[StorageBatchColumn.BALANCE_BEFORE] ?: 100.dp)  // ← Фиксированная ширина
```

---

## Порядок реализации

1. **StorageBatchColumn.kt** - enum для колонок партий
2. **StorageBatchColumns.kt** - создать колонки с поддержкой фикс. ширины
3. **StorageBatchesSection.kt** - компонент для отображения партий (принимает parentState)
4. **ProductStorageComponent.kt** - реализовать toggleExpand
5. **Columns.kt** - передать callback в createStorageColumns
6. **ImmutableListBox.kt** - добавить rowEmbedded support и передать tableState

---

## Проверка

1. Запустить desktop приложение: `./gradlew :app:desktopApp:run`
2. Открыть таблицу остатков на складах
3. Кликнуть на иконку разворачивания
4. Проверить:
   - Подстроки появляются с анимацией
   - **Ширина колонок партий совпадает с шириной колонок основной таблицы** (выравнивание по вертикали)
   - Есть отступ слева
   - Колонка EXPAND пустая у партий
   - При повторном клике подстроки скрываются
5. Изменить ширину колонки в основной таблице → проверить что подстроки тоже обновляются

---

## Зависимости

- Библиотека `table-kmp` уже поддерживает `rowEmbedded` параметр
- Структура данных `StorageProductUi` уже имеет `batches: List<StorageBatchUi>` и `expanded: Boolean`

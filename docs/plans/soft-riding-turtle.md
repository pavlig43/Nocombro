# План: Expandable партии в Storage таблице

## Шаг 0: Восстановить storage из immutable

Восстановить удалённые файлы из `features/table/immutable/.../items/storage/`:

```bash
git show 287bd8d0:features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/ProductStorageComponent.kt > ...
git show 287bd8d0:features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/ProductStorageUi.kt > ...
git show 287bd8d0:features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/items/storage/Columns.kt > ...
```

## Цель
Отобразить партии как **под-строки** в основной таблице (как в Excel `storage.xlsx`):
```
Продукт   | старт | приход | расход | остаток
Соль      | 15    | 20     | 10     | 25
  02.01   | 10    | 10     | 8      | 12
  05.01   | 5     | 10     | 2      | 13
```

## Текущая ситуация
- `StorageProductUi` содержит `batches: List<StorageBatchUi>` как вложенный список
- `StorageBatchesSection` — отдельная таблица для партий
- Не работает как единая таблица с под-строками

## Решение: Плоский список строк

### 1. Объединённая модель данных
Создать `StorageRow` — одна модель для продукта и партии:

```kotlin
sealed interface StorageRow {
    val composeId: Int
    val productId: Int
    val productName: String
    val balanceBeforeStart: Int
    val incoming: Int
    val outgoing: Int
    val balanceOnEnd: Int

    data class Product(
        override val composeId: Int,
        override val productId: Int,
        override val productName: String,
        override val balanceBeforeStart: Int,
        override val incoming: Int,
        override val outgoing: Int,
        override val balanceOnEnd: Int,
        val expanded: Boolean = false,
    ) : StorageRow

    data class Batch(
        override val composeId: Int,
        override val productId: Int,
        override val productName: String,
        val batchName: String,
        override val balanceBeforeStart: Int,
        override val incoming: Int,
        override val outgoing: Int,
        override val balanceOnEnd: Int,
    ) : StorageRow
}
```

### 2. Трансформация данных
Преобразовать `StorageProductUi` → `List<StorageRow>`:

```kotlin
fun StorageProductUi.toRows(): List<StorageRow> = buildList {
    // Строка продукта (сумма партий)
    add(StorageRow.Product(...))

    // Если развернут — партии
    if (expanded) {
        batches.forEach { batch ->
            add(StorageRow.Batch(...))
        }
    }
}
```

### 3. Изменение колонок
Добавить отступ для партий и кнопку expand:

```kotlin
column(StorageProductField.EXPAND, valueOf = { it is StorageRow.Product }) {
    cell { item, _ ->
        when (item) {
            is StorageRow.Product -> ExpandButton(item.expanded)
            is StorageRow.Batch -> Spacer() // пусто с отступом
        }
    }
}

column(StorageProductField.PRODUCT_NAME, valueOf = { it.productName }) {
    cell { item, _ ->
        when (item) {
            is StorageRow.Product -> Text(item.productName)
            is StorageRow.Batch -> Text(item.batchName, Modifier.padding(start = 24.dp))
        }
    }
}
```

### 4. Toggle expand
Обновлять состояние и пересобирать список:

```kotlin
fun onToggleExpand(productId: Int) {
    tableData = tableData.copy(
        displayedProducts = tableData.displayedProducts.map { product ->
            if (product.productId == productId) {
                product.copy(expanded = !product.expanded)
            } else {
                product
            }
        }.flatMap { it.toRows() }  // пересобрать плоский список
    )
}
```

## Файлы для изменения

1. **`StorageProductUi.kt`** — добавить метод `toRows()`
2. **`StorageTableData.kt`** — изменить `displayedProducts` на `List<StorageRow>`
3. **`StorageColumns.kt`** — обновить колонки для работы с `StorageRow`
4. **`StorageComponent.kt`** — обновить `toggleExpand` логику
5. **Удалить** `StorageBatchesSection.kt` — больше не нужна

## Вариант 2: Использовать `groupBy`

Если сделать все партии отдельными строками:
```kotlin
[
  StorageRow.Product("Соль", ...),
  StorageRow.Batch("Соль", "02.01", ...),
  StorageRow.Batch("Соль", "05.01", ...),
]
```

Тогда можно использовать `state.groupBy(StorageProductField.PRODUCT_NAME)` — но это не даёт expand/collapse, только группировку.

---

## Вопрос

Какой подход предпочитаешь?
1. **Плоский список** — полный контроль, custom логика expand
2. **`groupBy` + плоский список** — использовать встроенную группировку

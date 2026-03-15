# План: Добавить опцию "Не указан" (null) в фильтр Типа транзакции

## Задача
Добавить возможность фильтровать элементы с `transactionType == null` через опцию "Не указан" в списке фильтра.

## Проблема
- `ExpenseTableUi.transactionType` — это `TransactionType?` (nullable)
- `EnumTableFilter` из библиотеки принимает только non-null enum значения
- В UI фильтра нет опции для выбора null

## Решение: Создать wrapper тип для nullable enum

### Файлы для изменения

#### 1. `features/table/immutable/src/desktopMain/kotlin/ru/pavlig43/immutable/internal/component/items/expense/Columns.kt`

**Добавить sealed interface для nullable опций**:

```kotlin
// После импортов, перед enum class ExpenseField
sealed interface TransactionTypeFilterOption {
    val displayName: String

    data class Value(val type: TransactionType) : TransactionTypeFilterOption {
        override val displayName: String get() = type.displayName
    }

    data object Null : TransactionTypeFilterOption {
        override val displayName: String get() = "Не указан"
    }
}
```

**Изменить колонку TRANSACTION_TYPE**:
```kotlin
readEnumColumn(
    headerText = "Тип транзакции",
    column = ExpenseField.TRANSACTION_TYPE,
    valueOf = { it.transactionType },
    filterType = TableFilterType.EnumTableFilter(
        options = buildList {
            add(TransactionTypeFilterOption.Null)
            TransactionType.entries.forEach { add(TransactionTypeFilterOption.Value(it)) }
        }.toImmutableList(),
        getTitle = { it.displayName }
    ),
    getTitle = { it?.displayName ?: "Не указан" },
)
```

**Важно**: `EnumTableFilter` типизирован `TransactionTypeFilterOption`, а не `TransactionType`.

#### 2. `features/table/immutable/src/desktopMain/kotlin/ru/pavlig43/immutable/internal/component/items/expense/ExpenseFilterMatcher.kt`

**Изменить логику для TRANSACTION_TYPE**:

```kotlin
ExpenseField.TRANSACTION_TYPE -> {
    when (stateAny.constraint) {
        null -> true
        FilterConstraint.IN -> {
            @Suppress("UNCHECKED_CAST")
            val selected = stateAny.values as? List<TransactionTypeFilterOption> ?: emptyList()
            val itemMatches = when (item.transactionType) {
                null -> selected.any { it is TransactionTypeFilterOption.Null }
                else -> selected.any { it is TransactionTypeFilterOption.Value && it.type == item.transactionType }
            }
            selected.isEmpty() || itemMatches
        }
        FilterConstraint.NOT_IN -> {
            @Suppress("UNCHECKED_CAST")
            val selected = stateAny.values as? List<TransactionTypeFilterOption> ?: emptyList()
            val itemMatches = when (item.transactionType) {
                null -> selected.none { it is TransactionTypeFilterOption.Null }
                else -> selected.none { it is TransactionTypeFilterOption.Value && it.type == item.transactionType }
            }
            selected.isEmpty() || itemMatches
        }
        else -> true
    }
}
```

#### 3. `features/table/immutable/src/desktopMain/kotlin/ru/pavlig43/immutable/internal/component/items/expense/ExpenseSorter.kt`

**Обновить сортировку для nullable значений** (если нужно):

```kotlin
ExpenseField.TRANSACTION_TYPE -> {
    val aType = a.transactionType?.ordinal ?: Int.MAX_VALUE
    val bType = b.transactionType?.ordinal ?: Int.MAX_VALUE
    aType.compareTo(bType)
}
```

## Пошаговая реализация

1. Создать `TransactionTypeFilterOption` sealed interface в `Columns.kt`
2. Обновить `EnumTableFilter` с использованием нового типа опций
3. Обновить `ExpenseFilterMatcher.kt` для правильной обработки нового типа
4. (Опционально) Обновить сортировку

## Верификация

1. Открыть таблицу расходов
2. Нажать на фильтр колонки "Тип транзакции"
3. Убедиться что в списке первая опция "Не указан"
4. Выбрать только "Не указан" → должны показаться только элементы с `transactionType == null`
5. Выбрать "BUY" + "Не указан" → должны показаться элементы с BUY и null
6. Снять все фильтры → должны показаться все элементы

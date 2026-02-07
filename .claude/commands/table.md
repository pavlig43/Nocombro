---
name: table
description: Создание таблицы (mutable или immutable) для сущности
---

# Создание таблицы для: $ARGUMENTS

## 1. Тип таблицы

**Mutable** — для редактирования (`MutableTableBox`)
**Immutable** — только для просмотра (`ImmutableTableBox`)

## 2. Анализ

```bash
find . -name "*Entity.kt" | grep -i "$ARGUMENTS"
```

Проверь:
- `LocalDateTime` поля? → `.claude/rules/date-time-picker.md`
- `price`, `weight`, `cost` поля? → `.claude/rules/decimal-fields.md`
- `@Relation`? → `.claude/rules/database.md`

## 3. Выбор места выполнения

См. `.claude/rules/task-execution-scope.md`

```
Где выполнить?
1. Git Worktree (рекомендуется)
2. Новая ветка
3. Текущая ветка
```

## 4. Применение правил

| Тип поля | Что использовать |
|----------|-----------------|
| Дата/время | `DateTimeRow` из `coreui` |
| Деньги (рубли) | `decimalColumn` с `DecimalFormat.RUB()` |
| Вес (кг) | `decimalColumn` с `DecimalFormat.KG()` |
| Текст | `TextFieldRow` |
| Числа (Int) | Обычная колонка |

## 5. Структура

```
features/<feature>/
├── <Entity>Component.kt
├── <Entity>Screen.kt
└── columns/<Entity>Columns.kt
```

## 6. Пример колонок с разными типами

```kotlin
import ru.pavlig43.mutable.api.ui.decimalColumn
import ru.pavlig43.mutable.api.ui.DecimalFormat

override val columns = createColumns(
    onOpenDateTimeDialog = { composeId -> /* ... */ },
    onEvent = ::onEvent
)

// В createColumns():
fun createColumns(
    onOpenDateTimeDialog: (Int) -> Unit,
    onEvent: (MutableUiEvent) -> Unit
): ImmutableList<ColumnSpec<Item>> = immutableListOf(
    // Текст
    column(
        key = Columns.Name,
        header = "Название",
        getter = { it.name },
        cell = { _, item -> Text(item.name) }
    ),
    
    // Дата/время
    column(
        key = Columns.Date,
        header = "Дата",
        getter = { it.createdAt.toString() },
        cell = { column, item ->
            DateTimeRow(
                date = item.createdAt,
                isChangeDialogVisible = { onOpenDateTimeDialog(item.composeId) }
            )
        }
    ),
    
    // Вес (кг)
    decimalColumn(
        key = Columns.Weight,
        getValue = { it.weightInGrams },
        headerText = "Вес (кг)",
        decimalFormat = DecimalFormat.KG(),
        onEvent = onEvent,
        updateItem = { item, grams -> item.copy(weightInGrams = grams) }
    ),
    
    // Цена (₽)
    decimalColumn(
        key = Columns.Price,
        getValue = { it.priceInKopecks },
        headerText = "Цена (₽)",
        decimalFormat = DecimalFormat.RUB(),
        onEvent = onEvent,
        updateItem = { item, kopecks -> item.copy(priceInKopecks = kopecks) },
        footerValue = { items -> items.sumOf { it.priceInKopecks } }
    )
)
```

## Чек-лист

- [ ] Определил тип (mutable/immutable)
- [ ] Проверил поля
- [ ] Спросил где выполнить
- [ ] Применил правила для каждого типа поля
- [ ] Создал Component с SlotNavigation
- [ ] Создал Screen
- [ ] Добавил в DI

---
name: table
description: Создание таблицы (mutable или immutable) для сущности в Nocombro. Используй когда нужно создать таблицу для отображения данных.
---

# Создание таблицы для $ARGUMENTS

## 1. Определи тип таблицы

**Mutable таблица:**
- Для редактирования данных
- Используй `MutableTableBox` из `features/table/mutable/`

**Immutable таблица:**
- Только для просмотра
- Используй `ImmutableTableBox` из `features/table/immutable/`

## 2. Анализ сущности

```bash
find . -name "*Entity.kt" | grep -i "$ARGUMENTS"
```

**Проверь:**
- [ ] Есть ли `LocalDateTime`? → см. `.claude/rules/date-time-picker.md`
- [ ] Есть ли `@Relation`? → см. `.claude/rules/database.md`

## 3. Выбор места выполнения (ОБЯЗАТЕЛЬНО!)

См. `.claude/rules/task-execution-scope.md`

```
Где выполнить задачу?
1. Git Worktree (рекомендуется)
2. Новая ветка
3. Текущая ветка (только для мелких правок)
```

## 4. Применение правил

| Условие | Правило |
|---------|---------|
| Поля с датой/временем | `.claude/rules/date-time-picker.md` |
| Связанные сущности | `.claude/rules/database.md` |

## 5. Структура

```
features/<feature>/
├── <Entity>Component.kt
├── <Entity>Screen.kt
└── columns/
    └── <Entity>Columns.kt
```

## 6. Пример Component

```kotlin
internal class MyTableComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext {

    private val dialogNavigation = SlotNavigation<MyDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "my_dialog",
        serializer = MyDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    override val columns: ImmutableList<ColumnSpec<Item>> =
        createMyColumns(
            onOpenDateTimeDialog = { composeId ->
                dialogNavigation.activate(MyDialog.DateTimePicker(composeId))
            }
        )
}
```

## 7. Пример Screen

```kotlin
@Composable
internal fun MyTableScreen(component: MyTableComponent) {
    val dialog by component.dialog.subscribeAsState()

    MutableTableBox(
        component = component,
        columns = component.columns,
        items = component.items,
        onEvent = ::onEvent
    )

    // Dialogs
    dialog.child?.instance?.also { /* ... */ }
}
```

## Чек-лист

- [ ] Определил тип (mutable/immutable)
- [ ] Проверил Entity на поля с датой/временем
- [ ] Спросил пользователя где выполнить
- [ ] Применил соответствующие правила
- [ ] Создал Component с `SlotNavigation`
- [ ] Создал Screen
- [ ] Добавил в DI

## Примеры

| Что | Где |
|-----|-----|
| Mutable таблица | `features/sampletable/` |
| С датой/временем | `features/form/transaction/.../reminders/` |

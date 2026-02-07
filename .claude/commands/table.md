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
- `@Relation`? → `.claude/rules/database.md`

## 3. Выбор места выполнения

См. `.claude/rules/task-execution-scope.md`

```
Где выполнить?
1. Git Worktree (рекомендуется)
2. Новая ветка
3. Текущая ветка
```

## 4. Структура

```
features/<feature>/
├── <Entity>Component.kt
├── <Entity>Screen.kt
└── columns/<Entity>Columns.kt
```

## 5. Component с диалогами

```kotlin
internal class MyTableComponent(componentContext: ComponentContext) : ComponentContext by componentContext {
    private val dialogNavigation = SlotNavigation<MyDialog>()
    
    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "my_dialog",
        serializer = MyDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )
    
    override val columns = createMyColumns(
        onOpenDateTimeDialog = { composeId ->
            dialogNavigation.activate(MyDialog.DateTimePicker(composeId))
        }
    )
}
```

## Чек-лист

- [ ] Определил тип (mutable/immutable)
- [ ] Проверил поля
- [ ] Спросил где выполнить
- [ ] Применил правила
- [ ] Создал Component с SlotNavigation
- [ ] Создал Screen
- [ ] Добавил в DI

---
name: form
description: Создание формы для создания/редактирования сущности в Nocombro. Используй когда нужно создать форму ввода данных.
---

# Создание формы для $ARGUMENTS

## 1. Анализ сущности

```bash
find . -name "*Entity.kt" | grep -i "$ARGUMENTS"
```

**Проверь поля:**
- [ ] Текстовые → `TextFieldRow`
- [ ] Числа → `IntFieldRow`, `BigDecimalFieldRow`
- [ ] Дата/время → **см. `.claude/rules/date-time-picker.md`**
- [ ] Выбор из списка → `DropdownRow`
- [ ] Связанные сущности → **см. `.claude/rules/database.md`**

## 2. Выбор места выполнения

**ОБЯЗАТЕЛЬНО!** См. `.claude/rules/task-execution-scope.md`

## 3. Применение правил

| Условие | Правило |
|---------|---------|
| Поля `LocalDateTime` | `.claude/rules/date-time-picker.md` |
| `@Relation` поля | `.claude/rules/database.md` |

## 4. Компоненты форм из `coreui`

| Компонент | Для чего |
|-----------|----------|
| `TextFieldRow` | Текстовые поля |
| `IntFieldRow` | Целые числа |
| `BigDecimalFieldRow` | Десятичные числа |
| `DateTimeRow` | Дата + время |
| `DateRow` | Только дата |
| `DropdownRow` | Выбор из списка |

**Путь:** `coreui/src/commonMain/kotlin/ru/pavlig43/coreui/coreFieldBlock/`

## 5. Структура

```
features/<feature>/
├── <Entity>FormComponent.kt
├── <Entity>FormScreen.kt
└── model/
    └── <Entity>FormData.kt
```

## 6. Пример FormComponent

```kotlin
internal class MyFormComponent(
    componentContext: ComponentContext,
    private val onSave: (MyEntity) -> Unit
) : ComponentContext by componentContext {

    private val formData = mutableStateOf(MyEntity())
    private val dialogNavigation = SlotNavigation<FormDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "form_dialog",
        serializer = FormDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    fun onEvent(event: FormEvent) {
        when (event) {
            is FormEvent.Save -> onSave(formData.value)
            is FormEvent.UpdateField -> {
                formData.value = formData.value.copy(...)
            }
            is FormEvent.OpenDatePicker -> {
                dialogNavigation.activate(FormDialog.DatePicker)
            }
        }
    }
}
```

## 7. Пример FormScreen

```kotlin
@Composable
internal fun MyFormScreen(component: MyFormComponent) {
    val dialog by component.dialog.subscribeAsState()
    val data by component.formData

    Column {
        TextFieldRow(
            label = "Название",
            value = data.name,
            onValueChange = { component.onEvent(FormEvent.UpdateName(it)) }
        )

        DateTimeRow(
            date = data.createdAt,
            isChangeDialogVisible = {
                component.onEvent(FormEvent.OpenDatePicker)
            }
        )
    }

    // Dialogs
    dialog.child?.instance?.also { /* ... */ }
}
```

## Чек-лист

- [ ] Проанализировал поля сущности
- [ ] Проверил `.claude/rules/date-time-picker.md` (если есть дата/время)
- [ ] Спросил пользователя где выполнить
- [ ] Выбрал компоненты из `coreui`
- [ ] Создал FormComponent
- [ ] Создал FormScreen
- [ ] Добавил в DI

## Примеры

| Что | Где |
|-----|-----|
| Форма транзакции | `features/form/transaction/` |
| С DatePicker | `features/form/transaction/.../reminders/` |

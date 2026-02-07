---
name: form
description: Создание формы для создания/редактирования сущности
---

# Создание формы для: $ARGUMENTS

## 1. Анализ полей

```bash
find . -name "*Entity.kt" | grep -i "$ARGUMENTS"
```

Поля:
- Текст → `TextFieldRow`
- Числа → `IntFieldRow`, `BigDecimalFieldRow`
- Дата/время → см. `.claude/rules/date-time-picker.md`
- Список → `DropdownRow`

## 2. Компоненты из `coreui`

Путь: `coreui/src/commonMain/kotlin/ru/pavlig43/coreui/coreFieldBlock/`

| Компонент | Для чего |
|-----------|----------|
| TextFieldRow | Текст |
| IntFieldRow | Целые числа |
| BigDecimalFieldRow | Десятичные числа |
| DateTimeRow | Дата + время |
| DateRow | Только дата |
| DropdownRow | Выбор из списка |

## 3. Структура

```
features/<feature>/
├── <Entity>FormComponent.kt
├── <Entity>FormScreen.kt
└── model/<Entity>FormData.kt
```

## 4. FormComponent

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
            is FormEvent.UpdateField -> { /* ... */ }
            is FormEvent.OpenDatePicker -> {
                dialogNavigation.activate(FormDialog.DatePicker)
            }
        }
    }
}
```

## 5. FormScreen

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

    dialog.child?.instance?.also { /* ... */ }
}
```

## Чек-лист

- [ ] Проанализировал поля
- [ ] Проверил date-time-picker.md
- [ ] Спросил где выполнить
- [ ] Выбрал компоненты из coreui
- [ ] Создал FormComponent
- [ ] Создал FormScreen
- [ ] Добавил в DI

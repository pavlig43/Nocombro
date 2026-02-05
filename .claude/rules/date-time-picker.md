# Правила: Работа с датой и временем

При добавлении выбора даты/времени в формах используй следующую архитектуру:

## 1. UI Компоненты (в слое View)

**Используй готовые компоненты из `coreui`:**
- `DateTimeRow` — для отображения даты+времени с иконкой
- `DateRow` — для отображения только даты с иконкой

**Путь:** `coreui/src/commonMain/kotlin/ru/pavlig43/coreui/coreFieldBlock/DateTimeFieldBlock.kt`

### Пример использования (простой вариант):

```kotlin
@Composable
fun MyScreen() {
    var isDateTimePickerVisible by remember { mutableStateOf(false) }

    DateTimeRow(
        date = myDateTime,
        isChangeDialogVisible = { isDateTimePickerVisible = !isDateTimePickerVisible }
    )

    if (isDateTimePickerVisible) {
        DateTimePickerDialog(
            onDismissRequest = { isDateTimePickerVisible = false },
            dateTime = myDateTime,
            onSelectDateTime = { newDateTime -> /* обработка */ }
        )
    }
}
```

### Доступные диалоги

| Диалог | Для чего | Путь |
|--------|---------|------|
| `DateTimePickerDialog` | Выбор даты и времени | `coreui/.../DateTimePickerDialog.kt` |
| `DatePickerDialog` | Выбор только даты | `coreui/.../DatePickerDialog.kt` |

---

## 2. Компоненты Decompose (в слое Component)

Для управления диалогами через Decompose используй `SlotNavigation`.

### Полный пример реализации:

```kotlin
internal class MyComponent(
    componentContext: ComponentContext,
    // ...
) : ComponentContext by componentContext {

    // 1. Создай навигацию для диалогов
    private val dialogNavigation = SlotNavigation<MyDialog>()

    // 2. Создай child slot
    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "my_dialog",
        serializer = MyDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    // 3. Фабрика для создания диалогов
    private fun createDialogChild(dialogConfig: MyDialog, context: ComponentContext): DialogChild {
        return when (dialogConfig) {
            is MyDialog.DateTimePicker -> {
                val item = itemList.value.first { it.composeId == dialogConfig.composeId }
                val dateTimeComponent = DateTimeComponent(
                    componentContext = context,
                    initDatetime = item.dateTime,
                    onDismissRequest = { dialogNavigation.dismiss() },
                    onChangeDate = { newDateTime ->
                        onEvent(UpdateItem(item.copy(dateTime = newDateTime)))
                    }
                )
                DialogChild.DateTime(dateTimeComponent)
            }
        }
    }

    // 4. В columns передай callback для открытия диалога
    override val columns: ImmutableList<ColumnSpec<...>> = createMyColumns(
        onOpenDateTimeDialog = { composeId ->
            dialogNavigation.activate(MyDialog.DateTimePicker(composeId))
        },
        onEvent = ::onEvent
    )
}

// 5. Определи конфигурацию диалога
@Serializable
internal sealed interface MyDialog {
    @Serializable
    data class DateTimePicker(val composeId: Int) : MyDialog
}

// 6. Определи типы дочерних компонентов
sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}
```

---

## 3. В UI экране

Подпишись на изменения диалога и отображай его:

```kotlin
@Composable
internal fun MyScreen(component: MyComponent) {
    val dialog by component.dialog.subscribeAsState()

    // Основной UI...
    MutableTableBox(component, ...)

    // Отображение диалога
    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}
```

---

## Ссылки на полные примеры

| Что | Файл |
|-----|-------|
| **Компонент** | `features/form/transaction/.../reminders/RemindersComponent.kt` |
| **Экран** | `features/form/transaction/.../reminders/RemindersScreen.kt` |

---

## ⚠️ ВАЖНО

1. **Всегда используй этот паттерн** для выбора даты/времени
2. **Не создавай новые диалоги** — используй существующие компоненты из `coreui`
3. **Используй `SlotNavigation`** для управления диалогами в Decompose
4. **Передавай `composeId`** в диалог, чтобы знать какой элемент редактируется

---

## Дополнительные компоненты

### DateTimeComponent

Готовый компонент-обёртка для управления состоянием диалога даты/времени:

```kotlin
DateTimeComponent(
    componentContext = context,
    initDatetime = item.dateTime,
    onDismissRequest = { /* закрыть диалог */ },
    onChangeDate = { newDateTime -> /* сохранить */ }
)
```

Используется внутри `createDialogChild` для создания компонента диалога.

# Правила: Работа с диалогами в Decompose

Для управления диалогами (DatePicker, подтверждения, выбор элемента) в Decompose используй `SlotNavigation`.

---

## Когда нужно

- Выбор даты/времени
- Подтверждение удаления
- Выбор элемента из списка
- Любой диалог, требующий состояния

---

## Структура

### 1. Создай навигацию и child slot

```kotlin
internal class MyComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext {

    // Навигация для диалогов
    private val dialogNavigation = SlotNavigation<MyDialog>()

    // Child slot для отображения
    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "my_dialog",
        serializer = MyDialog.serializer(),
        handleBackButton = true,  // Закрывать по Back
        childFactory = ::createDialogChild
    )
}
```

### 2. Определи конфигурацию диалога

```kotlin
@Serializable
internal sealed interface MyDialog {
    @Serializable
    data class ConfirmDelete(val itemId: Int) : MyDialog

    @Serializable
    data class SelectOption(val composeId: Int) : MyDialog
}
```

### 3. Создай фабрику для диалогов

```kotlin
private fun createDialogChild(dialogConfig: MyDialog, context: ComponentContext): DialogChild {
    return when (dialogConfig) {
        is MyDialog.ConfirmDelete -> {
            val component = ConfirmDialogComponent(
                componentContext = context,
                title = "Удалить элемент?",
                onConfirm = { /* удалить */ },
                onDismiss = { dialogNavigation.dismiss() }
            )
            DialogChild.Confirm(component)
        }
    }
}
```

### 4. Определи типы дочерних компонентов

```kotlin
sealed interface DialogChild {
    class Confirm(val component: ConfirmDialogComponent) : DialogChild
}
```

### 5. Открывай диалог

```kotlin
// В columns или обработчике событий
onOpenDialog = { itemId ->
    dialogNavigation.activate(MyDialog.ConfirmDelete(itemId))
}

// Закрыть диалог
dialogNavigation.dismiss()
```

---

## В UI экране

Подпишись на диалог и отображай его:

```kotlin
@Composable
internal fun MyScreen(component: MyComponent) {
    val dialog by component.dialog.subscribeAsState()

    // Основной UI...

    // Отображение диалога
    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.Confirm -> ConfirmDialog(dialogChild.component)
        }
    }
}
```

---

## Примеры в проекте

| Что | Файл |
|-----|-------|
| DateTimePicker | `features/form/transaction/.../reminders/RemindersComponent.kt` |
| DatePicker | `coreui/.../DatePickerDialog.kt` |

---

## ⚠️ ВАЖНО

1. **Используй `SlotNavigation`** для всех диалогов в Decompose
2. **`handleBackButton = true`** — диалог должен закрываться по Back
3. **Передавай параметры** в конфигурацию (itemId, composeId и т.д.)
4. **`@Serializable`** — конфигурация должна сериализоваться
5. **Вызывай `dismiss()`** после закрытия диалога

---

## Шаблон для копирования

```kotlin
// 1. Navigation
private val dialogNavigation = SlotNavigation<MyDialog>()
internal val dialog = childSlot(source = dialogNavigation, ...)

// 2. Config
@Serializable
sealed interface MyDialog {
    @Serializable
    data class MyDialogName(val param: Type) : MyDialog
}

// 3. Child types
sealed interface DialogChild {
    class MyDialogName(val component: MyDialogComponent) : DialogChild
}

// 4. Factory
private fun createDialogChild(config: MyDialog, context: ComponentContext): DialogChild {
    return when (config) {
        is MyDialog.MyDialogName -> {
            DialogChild.MyDialogName(MyDialogComponent(context, ...))
        }
    }
}

// 5. Open
dialogNavigation.activate(MyDialog.MyDialogName(param))

// 6. Close
dialogNavigation.dismiss()
```

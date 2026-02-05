# Изменения для исправления FOREIGN KEY constraint failed

## Описание проблемы
Ошибка `FOREIGN KEY constraint failed` возникала при попытке сохранить reminder в базу данных, потому что `ReminderBD` не содержал поле `transactionId`, необходимое для внешнего ключа.

## Решение
Добавлено поле `transactionId` в `ReminderBD` и обновлены все маппинги.

## Измененные файлы

### 1. database/src/commonMain/kotlin/ru/pavlig43/database/data/transaction/reminder/ReminderBD.kt
**Изменение**: Добавлено поле `transactionId: Int`

```kotlin
// Было:
data class ReminderBD(
    val text: String,
    val reminderDateTime: LocalDateTime,
    override val id: Int
) : CollectionObject

// Стало:
data class ReminderBD(
    val transactionId: Int,
    val text: String,
    val reminderDateTime: LocalDateTime,
    override val id: Int
) : CollectionObject
```

### 2. features/form/transaction/src/commonMain/kotlin/ru/pavlig43/transaction/internal/di/CreateTransactionFormModule.kt
**Изменения**:
1. Обновлен маппинг `Reminder.toReminderBD()` - добавлено поле `transactionId`
2. Изменен метод `ReminderBD.toReminder()` - больше не принимает параметры, использует `transactionId` из `ReminderBD`
3. Обновлен `upsertCollection` - убран параметр `transactionId` из lambda

```kotlin
// Маппинг из Entity в BD
private fun Reminder.toReminderBD() = ReminderBD(
    transactionId = transactionId,  // Добавлено
    text = text,
    reminderDateTime = reminderDateTime,
    id = id
)

// Маппинг из BD в Entity
private fun ReminderBD.toReminder() = Reminder(  // Больше не принимает параметры
    transactionId = transactionId,  // Использует поле из ReminderBD
    text = text,
    reminderDateTime = reminderDateTime,
    id = id
)

// Репозиторий
upsertCollection = { reminders ->
    dao.upsertAll(reminders.map { it.toReminder() })  // Убран параметр
}
```

### 3. features/form/transaction/src/commonMain/kotlin/ru/pavlig43/transaction/internal/component/tabs/component/reminders/RemindersUi.kt
**Изменение**: Добавлено поле `transactionId: Int`

```kotlin
// Было:
data class RemindersUi(
    override val composeId: Int,
    val id: Int,
    val text: String,
    val reminderDateTime: LocalDateTime
) : ITableUi

// Стало:
data class RemindersUi(
    override val composeId: Int,
    val id: Int,
    val transactionId: Int,  // Добавлено
    val text: String,
    val reminderDateTime: LocalDateTime
) : ITableUi
```

### 4. features/form/transaction/src/commonMain/kotlin/ru/pavlig43/transaction/internal/component/tabs/component/reminders/RemindersComponent.kt
**Изменения**:
1. Добавлено поле класса `private val transactionId = transactionId` для захвата параметра конструктора
2. Обновлен `createNewItem()` - использует `transactionId` из класса
3. Обновлен `ReminderBD.toUi()` - передает `transactionId` из BD
4. Обновлен `RemindersUi.toBDIn()` - передает `transactionId` из UI

```kotlin
internal class RemindersComponent(
    componentContext: ComponentContext,
    transactionId: Int,
    repository: UpdateCollectionRepository<ReminderBD, ReminderBD>
) : MutableTableComponent<...>(...) {
    private val transactionId = transactionId  // Захватываем параметр

    override fun createNewItem(composeId: Int): RemindersUi {
        return RemindersUi(
            composeId = composeId,
            id = 0,
            transactionId = transactionId,  // Используем захваченное значение
            text = "",
            reminderDateTime = emptyLocalDateTime
        )
    }

    override fun ReminderBD.toUi(composeId: Int): RemindersUi {
        return RemindersUi(
            composeId = composeId,
            transactionId = transactionId,  // Передаем из BD
            text = text,
            reminderDateTime = reminderDateTime,
            id = id
        )
    }

    override fun RemindersUi.toBDIn(): ReminderBD {
        return ReminderBD(
            transactionId = transactionId,  // Передаем из UI
            text = text,
            reminderDateTime = reminderDateTime,
            id = id
        )
    }
}
```

### 5. features/form/transaction/src/commonMain/kotlin/ru/pavlig43/transaction/internal/component/tabs/component/reminders/Column.kt
**Изменение**: Использована стандартная функция `DateTimeRow` из `coreui` вместо кастомной реализации

```kotlin
// Импорт
import ru.pavlig43.coreui.coreFieldBlock.DateTimeRow

// Использование
cell { item, _ ->
    DateTimeRow(
        date = item.reminderDateTime,  // Параметр называется 'date'
        isChangeDialogVisible = { onOpenDateTimeDialog(item.composeId) }
    )
}
```

## Резюме
- Добавлено поле `transactionId` во все слои: BD, UI, Entity
- Обновлены все маппинги для передачи `transactionId`
- Теперь reminder сохраняется с корректным внешним ключом на transaction
- FOREIGN KEY constraint больше не нарушается

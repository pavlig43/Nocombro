package ru.pavlig43.transaction.internal.update.tabs.component.reminders

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateTimeComponent
import ru.pavlig43.core.emptyLocalDateTime
import ru.pavlig43.database.data.transaction.reminder.ReminderBD
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent.UpdateItem
import ru.pavlig43.mutable.api.singleLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class RemindersComponent(
    componentContext: ComponentContext,
    private val transactionId: Int,
    repository: UpdateCollectionRepository<ReminderBD, ReminderBD>
) : MutableTableComponent<ReminderBD, ReminderBD, RemindersUi, RemindersField>(
    componentContext = componentContext,
    parentId = transactionId,
    title = "Напоминания",
    sortMatcher = RemindersSorter,
    filterMatcher = RemindersFilterMatcher,
    repository = repository
) {
    private val dialogNavigation = SlotNavigation<RemindersDialogChild>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "reminders_dialog",
        serializer = RemindersDialogChild.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    private fun createDialogChild(dialogConfig: RemindersDialogChild, context: ComponentContext): DialogChild {
        return when (dialogConfig) {
            is RemindersDialogChild.DateTimePicker -> {
                val item = itemList.value.first { it.composeId == dialogConfig.composeId }
                val dateTimeComponent = DateTimeComponent(
                    componentContext = context,
                    initDatetime = item.reminderDateTime,
                    onDismissRequest = { dialogNavigation.dismiss() },
                    onChangeDate = {
                        onEvent(UpdateItem(item.copy(reminderDateTime = it)))
                    }
                )
                DialogChild.DateTime(dateTimeComponent)
            }
        }
    }

    override val columns: ImmutableList<ColumnSpec<RemindersUi, RemindersField, TableData<RemindersUi>>> =
        createRemindersColumns(
            onOpenDateTimeDialog = { dialogNavigation.activate(RemindersDialogChild.DateTimePicker(it)) },
            onEvent = ::onEvent
        )

    override fun createNewItem(composeId: Int): RemindersUi {
        return RemindersUi(
            composeId = composeId,
            id = 0,
            transactionId = transactionId,
            text = "",
            reminderDateTime = emptyLocalDateTime
        )
    }

    override fun ReminderBD.toUi(composeId: Int): RemindersUi {
        return RemindersUi(
            composeId = composeId,
            transactionId = transactionId,
            text = text,
            reminderDateTime = reminderDateTime,
            id = id
        )
    }

    override fun RemindersUi.toBDIn(): ReminderBD {
        return ReminderBD(
            transactionId = transactionId,
            text = text,
            reminderDateTime = reminderDateTime,
            id = id
        )
    }

    override val errorMessages: Flow<List<String>> = itemList.map { lst ->
        buildList {
            lst.forEach { reminderUi ->
                val place = "В строке ${reminderUi.composeId + 1}"
                if (reminderUi.text.isBlank()) add("$place пустой текст напоминания")
                if (reminderUi.reminderDateTime == emptyLocalDateTime) add("$place не выбрана дата и время")
            }
        }
    }
}

@Serializable
internal sealed interface RemindersDialogChild {
    @Serializable
    data class DateTimePicker(val composeId: Int) : RemindersDialogChild
}

sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}

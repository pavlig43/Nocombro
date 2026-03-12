@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.transaction.internal.update.tabs.component.reminders

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.column.writeDateTimeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

enum class RemindersField {
    SELECTION,
    COMPOSE_ID,
    TEXT,
    REMINDER_DATE_TIME
}

internal fun createRemindersColumns(
    onOpenDateTimeDialog: (Int) -> Unit,
    onEvent: (MutableUiEvent) -> Unit
): ImmutableList<ColumnSpec<RemindersUi, RemindersField, TableData<RemindersUi>>> {
    val columns =
        editableTableColumns<RemindersUi, RemindersField, TableData<RemindersUi>> {

            idWithSelection(
                selectionKey = RemindersField.SELECTION,
                idKey = RemindersField.COMPOSE_ID,
                onEvent = onEvent
            )
            writeTextColumn(
                headerText = "Текст напоминания",
                column = RemindersField.TEXT,
                valueOf = {it.text},
                onChangeItem = {item,text -> onEvent(MutableUiEvent.UpdateItem(item.copy(text = text))) },
            )

            writeDateTimeColumn(
                headerText = "Дата/время",
                column = RemindersField.REMINDER_DATE_TIME,
                valueOf = {it.reminderDateTime},
                onOpenDateTimeDialog = { onOpenDateTimeDialog(it.composeId) },
            )


        }
    return columns
}

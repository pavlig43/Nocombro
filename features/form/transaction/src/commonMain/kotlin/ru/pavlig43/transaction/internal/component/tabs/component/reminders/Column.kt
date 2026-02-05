@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.transaction.internal.component.tabs.component.reminders

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.coreui.DateTimeRow
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.mutable.api.ui.idWithSelection
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableCellTextField
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

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

            column(RemindersField.TEXT, valueOf = { it.text }) {
                header("Текст напоминания")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.text) }
                editCell { item, _, _ ->
                    TableCellTextField(
                        value = item.text,
                        onValueChange = { onEvent(MutableUiEvent.UpdateItem(item.copy(text = it))) }
                    )
                }
                sortable()
            }

            column(RemindersField.REMINDER_DATE_TIME, { it.reminderDateTime }) {
                header("Дата и время")
                align(Alignment.Center)
                cell { item, _ ->
                    DateTimeRow(
                        date = item.reminderDateTime,
                        isChangeDialogVisible = { onOpenDateTimeDialog(item.composeId) }
                    )
                }
                sortable()
            }
        }
    return columns
}

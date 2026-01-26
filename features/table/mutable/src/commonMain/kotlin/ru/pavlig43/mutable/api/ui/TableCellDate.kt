package ru.pavlig43.mutable.api.ui

import androidx.compose.ui.Alignment
import kotlinx.datetime.LocalDate
import ru.pavlig43.coreui.DatePicker
import ru.pavlig43.coreui.coreFieldBlock.DateFieldBlock
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

fun <T : ITableUi, C, E : TableData<T>> EditableTableColumnsBuilder<T, C, E>.dateColumn(
    key: C,
    getValue: (T) -> LocalDate,
    headerText: String,
    onEvent: (MutableUiEvent.UpdateItem) -> Unit,
    updateItem: (T, LocalDate) -> T
) {
    column(key, valueOf = { getValue(it) }) {
        header(headerText)
        align(Alignment.Center)
        filter(
            TableFilterType.DateTableFilter()
        )
        cell { item, _ ->
            DatePicker(
                date = getValue(item),
                onSelectDate = { date ->
                    onEvent(
                        MutableUiEvent.UpdateItem(
                            updateItem(
                                item,
                                date
                            )
                        )
                    )
                },
            )
        }

        sortable()
    }
}
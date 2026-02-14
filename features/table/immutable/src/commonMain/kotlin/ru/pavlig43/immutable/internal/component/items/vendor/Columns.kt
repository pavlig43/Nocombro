@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.vendor

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class VendorField {

    SELECTION,
    NAME,
    ID,
    COMMENT
}

internal fun createVendorColumn(
    onEvent:(ImmutableTableUiEvent)-> Unit
): ImmutableList<ColumnSpec<VendorTableUi, VendorField, TableData<VendorTableUi>>> {
    val columns =
        tableColumns<VendorTableUi, VendorField, TableData<VendorTableUi>> {

            idWithSelection(
                selectionKey = VendorField.SELECTION,
                idKey = VendorField.ID,
                onEvent = onEvent
            )
            column(VendorField.NAME, valueOf = { it.displayName }) {
                header("Название")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item.displayName) }
                sortable()
            }
            column(VendorField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { document, _ -> Text(document.comment) }
            }
        }
    return columns

}

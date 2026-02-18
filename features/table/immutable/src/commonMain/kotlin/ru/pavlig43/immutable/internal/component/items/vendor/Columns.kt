@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.vendor

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
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

            readTextColumn(
                headerText = "Название",
                column = VendorField.NAME,
                valueOf = { it.displayName }
            )

            readTextColumn(
                headerText = "Комментарий",
                column = VendorField.COMMENT,
                valueOf = { it.comment }
            )
        }
    return columns

}

@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.declaration

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readDateColumn
import ru.pavlig43.immutable.internal.column.readIsActualColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.tableColumns

internal enum class DeclarationField {

    SELECTION,

    ID,
    NAME,
    VENDOR_NAME,
    BEST_BEFORE,
    IS_ACTUAL
}

@Suppress("LongMethod")
internal fun createDeclarationColumn(
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<DeclarationTableUi, DeclarationField, TableData<DeclarationTableUi>>> {
    val columns =
        tableColumns<DeclarationTableUi, DeclarationField, TableData<DeclarationTableUi>> {

            idWithSelection(
                selectionKey = DeclarationField.SELECTION,
                idKey = DeclarationField.ID,
                onEvent = onEvent
            )

            readTextColumn(
                headerText = "Название",
                column = DeclarationField.NAME,
                valueOf = { it.displayName }
            )

            readTextColumn(
                headerText = "Поставщик",
                column = DeclarationField.VENDOR_NAME,
                valueOf = { it.vendorName }
            )

            readDateColumn(
                headerText = "Годна до",
                column = DeclarationField.BEST_BEFORE,
                valueOf = { it.bestBefore }
            )

            readIsActualColumn(
                headerText = "Актуальность",
                column = DeclarationField.IS_ACTUAL,
                valueOf = { it.isActual }
            )
        }
    return columns

}

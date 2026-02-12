@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.product.internal.update.tabs.composition

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.column.readItemTypeColumn
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

internal enum class CompositionField {
    COMPOSE_ID,
    SELECTION,
    PRODUCT_NAME,
    PRODUCT_TYPE,

    COUNT
}

internal fun createCompositionColumn(
    onOpenProductDialog: (Int) -> Unit,
    onEvent: (MutableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<CompositionUi, CompositionField, TableData<CompositionUi>>> {
    val columns =
        editableTableColumns<CompositionUi, CompositionField, TableData<CompositionUi>> {


            idWithSelection(
                selectionKey = CompositionField.SELECTION,
                idKey = CompositionField.COMPOSE_ID,
                onEvent = onEvent
            )

            textWithSearchIconColumn(
                headerText = "Название",
                column = CompositionField.PRODUCT_NAME,
                valueOf = { it.productName },
                onOpenDialog = { onOpenProductDialog(it.composeId) },
                filterType = TableFilterType.TextTableFilter()
            )

            readItemTypeColumn(
                headerText = "Тип",
                column = CompositionField.PRODUCT_TYPE,
                valueOf = { it.productType },
                filterType = TableFilterType.EnumTableFilter(
                    options = ProductType.entries.toImmutableList(),
                    getTitle = { it.displayName }
                )
            )

            decimalColumn(
                key = CompositionField.COUNT,
                getValue = { it.count },
                headerText = "Количество",
                decimalFormat = DecimalFormat.KG(),
                updateItem = { item, count -> onEvent(MutableUiEvent.UpdateItem(item.copy(count = count))) },

            )


        }
    return columns

}

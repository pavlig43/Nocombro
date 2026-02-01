@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.product.internal.component.tabs.component.composition

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.NameRowWithSearchIcon
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.mutable.api.ui.DecimalFormat
import ru.pavlig43.mutable.api.ui.decimalColumn
import ru.pavlig43.mutable.api.ui.idWithSelection
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
            column(CompositionField.PRODUCT_NAME, { it.productName }) {
                header("Название")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ ->
                    NameRowWithSearchIcon(
                        text = item.productName,
                        onOpenChooseDialog = { onOpenProductDialog(item.composeId) }
                    )

                }

                sortable()
            }

            column(CompositionField.PRODUCT_TYPE, { it.productType }) {
                header("Тип")
                align(Alignment.Center)
                filter(
                    TableFilterType.EnumTableFilter(
                        options = ProductType.entries.map { it.enumValue }.toImmutableList(),
                        getTitle = { it.displayName }

                    ),
                )
                cell { item, _ -> Text(item.productType?.displayName ?: "") }

                sortable()
            }

            decimalColumn(
                key = CompositionField.COUNT,
                getValue = { it.count },
                headerText = "Количество",
                decimalFormat = DecimalFormat.KG(),
                onEvent = { updateEvent -> onEvent(updateEvent) },
                updateItem = { item, count -> item.copy(count = count) }
            )


        }
    return columns

}

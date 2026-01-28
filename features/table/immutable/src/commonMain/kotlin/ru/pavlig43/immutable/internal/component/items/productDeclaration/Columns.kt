package ru.pavlig43.immutable.internal.component.items.productDeclaration

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.tooltip.ToolTipProject
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.ui.idWithSelection
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class ProductDeclarationField {

    SELECTION,
    ID,
    DISPLAY_NAME,
    VENDOR_NAME,
    IS_ACTUAL
}

internal fun createProductDeclarationColumn(
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>>> {
    val columns =
        tableColumns<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>> {


            idWithSelection(
                selectionKey = ProductDeclarationField.SELECTION,
                idKey = ProductDeclarationField.ID,
                onEvent = onEvent
            )

            column(ProductDeclarationField.DISPLAY_NAME, valueOf = { it.displayName }) {
                header("Декларация")
                align(Alignment.Center)
                cell { item, _ -> Text(item.displayName) }
                sortable()
            }

            column(ProductDeclarationField.VENDOR_NAME, valueOf = { it.vendorName }) {
                header("Поставщик")
                align(Alignment.Center)
                cell { item, _ -> Text(item.vendorName) }
                sortable()
            }
            column(ProductDeclarationField.IS_ACTUAL, valueOf = { it.isActual }) {
                header("Актуальность")
                align(Alignment.Center)
                filter(TableFilterType.BooleanTableFilter())
                cell { item, _ ->
                    ToolTipProject(
                        tooltipText = if (item.isActual) "Aктуальна" else "Срок истек",
                        content = {
                            Icon(
                                if (item.isActual) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (item.isActual) Color.Green else Color.Red
                            )
                        }

                    )
                }
                sortable()
            }
        }
    return columns
}

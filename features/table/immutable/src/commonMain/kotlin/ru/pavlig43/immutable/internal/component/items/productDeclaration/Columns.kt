@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.productDeclaration

import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.tooltip.ToolTipProject
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.check
import ru.pavlig43.theme.close
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

            readTextColumn(
                headerText = "Декларация",
                column = ProductDeclarationField.DISPLAY_NAME,
                valueOf = { it.displayName }
            )

            readTextColumn(
                headerText = "Поставщик",
                column = ProductDeclarationField.VENDOR_NAME,
                valueOf = { it.vendorName }
            )

            // Custom column with tooltip for isActual
            column(ProductDeclarationField.IS_ACTUAL, valueOf = { it.isActual }) {
                header("Актуальность")
                align(Alignment.Center)
                filter(TableFilterType.BooleanTableFilter())
                cell { item, _ ->
                    ToolTipProject(
                        tooltipText = if (item.isActual) "Aктуальна" else "Срок истек",
                        content = {
                            Icon(
                                painterResource(if (item.isActual) Res.drawable.check else Res.drawable.close),
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

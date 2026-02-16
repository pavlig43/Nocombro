@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.product.internal.update.tabs.declaration

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.tooltip.ToolTipProject
import ru.pavlig43.flowImmutable.api.component.FlowMultiLineEvent
import ru.pavlig43.flowImmutable.api.component.column.idWithSelection
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
    DECLARATION_NAME,
    VENDOR_NAME,
    IS_ACTUAL
}

internal fun createProductDeclarationColumn(
    onEvent: (FlowMultiLineEvent) -> Unit,
    onCallAddDialog: () -> Unit,
): ImmutableList<ColumnSpec<FlowProductDeclarationTableUi, ProductDeclarationField, TableData<FlowProductDeclarationTableUi>>> {
    val columns =
        tableColumns<FlowProductDeclarationTableUi, ProductDeclarationField, TableData<FlowProductDeclarationTableUi>> {
            idWithSelection(
                selectionKey = ProductDeclarationField.SELECTION,
                idKey = ProductDeclarationField.ID,
                onCallAddDialog = onCallAddDialog,
                onEvent = onEvent
            )

            column(ProductDeclarationField.DECLARATION_NAME, valueOf = { it.declarationName }) {
                header("Декларация")
                align(Alignment.Center)
                cell { item, _ -> Text(item.declarationName) }
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

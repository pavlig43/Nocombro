@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.product.internal.update.tabs.declaration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.tooltip.ToolTipProject
import ru.pavlig43.tablecore.manger.SelectionUiEvent

import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.createButtonNew
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.check
import ru.pavlig43.theme.close
import ua.wwind.table.ColumnSpec
import ua.wwind.table.tableColumns

internal enum class ProductDeclarationField {
    SELECTION,
    DECLARATION_NAME,
    VENDOR_NAME,
    IS_ACTUAL
}

@Suppress("LongMethod")
internal fun createProductDeclarationColumn(
    onEvent: (ProductDeclarationEvent) -> Unit,
): ImmutableList<ColumnSpec<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>>> {
    val columns =
        tableColumns<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>> {

            column(ProductDeclarationField.SELECTION, valueOf = { it.composeId }) {
                width(48.dp)
                align(Alignment.Center)
                autoWidth(48.dp)
                header {
                    createButtonNew {
                        onEvent(ProductDeclarationEvent.AddNew)
                    }
                }
                cell { item, tableData ->
                    if (tableData.isSelectionMode) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Checkbox(
                                checked = item.composeId in tableData.selectedIds,
                                onCheckedChange = {
                                    onEvent(
                                        ProductDeclarationEvent.Selection(
                                            SelectionUiEvent.ToggleSelection(
                                                item.composeId
                                            )
                                        )
                                    )
                                },
                            )
                        }

                    }

                }
            }

            column(ProductDeclarationField.DECLARATION_NAME, valueOf = { it.declarationName }) {
                header("Декларация")
                align(Alignment.Center)
                cell { item, _ -> Text(item.declarationName) }
            }

            column(ProductDeclarationField.VENDOR_NAME, valueOf = { it.vendorName }) {
                header("Поставщик")
                align(Alignment.Center)
                cell { item, _ -> Text(item.vendorName) }
            }

            column(ProductDeclarationField.IS_ACTUAL, valueOf = { it.isActual }) {
                header("Актуальность")
                align(Alignment.Center)
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
            }
        }
    return columns
}

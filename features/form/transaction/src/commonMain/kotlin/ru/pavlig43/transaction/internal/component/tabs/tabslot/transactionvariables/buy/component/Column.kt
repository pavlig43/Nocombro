package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.itemlist.internal.component.SelectionUiEvent
import ru.pavlig43.itemlist.internal.model.TableData
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.BuyBaseUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

enum class BuyBaseField {
    SELECTION,
    COMPOSE_KEY,
    PRODUCT_NAME,
    DECLARATION_NAME,
    VENDOR_NAME,
    DATE_BORN,
    PRICE,
    COMMENT
}


@Suppress("LongMethod")
internal fun createBuyBaseColumn(
    onCreate: () -> Unit,
    onCallProductDialog: () -> Unit,
    onEvent: (SelectionUiEvent) -> Unit,
): ImmutableList<ColumnSpec<BuyBaseUi, BuyBaseField, TableData<BuyBaseUi>>> {
    val columns =
        tableColumns<BuyBaseUi, BuyBaseField, TableData<BuyBaseUi>> {


            column(BuyBaseField.SELECTION, valueOf = { it.composeId }) {

                title { createButtonNew(onCreate) }
                autoWidth(48.dp)
                cell { row, tableData ->
                    Box(
                        contentAlignment = Alignment.Companion.Center,
                        modifier = Modifier.Companion.fillMaxSize(),
                    ) {
                        Checkbox(
                            checked = row.composeId in tableData.selectedIds,
                            onCheckedChange = {
                                onEvent(SelectionUiEvent.ToggleSelection(row.composeId))
                            },
                        )
                    }


                }

            }

            column(BuyBaseField.COMPOSE_KEY, valueOf = { it.composeId }) {
                header("Ид")
                align(Alignment.Companion.Center)
                cell { row, _ -> Text(row.composeId.toString()) }
                // Enable built‑in Text filter UI in header
                // Auto‑fit to content with optional max cap
                autoWidth(max = 500.dp)

            }

            column(BuyBaseField.PRODUCT_NAME, valueOf = { it.productName }) {
                header("Продукт")
                align(Alignment.Companion.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ ->
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(row.productName)
                        IconButtonToolTip(
                            tooltipText = "Добавить",
                            onClick = onCallProductDialog,
                            icon = Icons.Default.Search,
                        )
                    }
                }
                sortable()
            }

            column(BuyBaseField.DECLARATION_NAME, valueOf = { it.declarationName }) {
                header("Декларация")
                align(Alignment.Companion.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.declarationName) }
                sortable()
            }
            column(BuyBaseField.VENDOR_NAME, valueOf = { it.vendorName }) {
                header("Поставщик")
                align(Alignment.Companion.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.vendorName) }
                sortable()
            }

            column(BuyBaseField.DATE_BORN, valueOf = { it.dateBorn }) {
                header("Дата производства")
                align(Alignment.Companion.Center)
                filter(TableFilterType.DateTableFilter())
                cell { row, _ ->
                    Text(row.dateBorn.format(dateFormat))
                }
                sortable()
            }
            column(BuyBaseField.PRICE, valueOf = { it.price }) {
                header("Цена")
                align(Alignment.Companion.Center)
                filter(TableFilterType.DateTableFilter())
                cell { row, _ ->
                    Text(row.dateBorn.format(dateFormat))
                }
                sortable()
            }
            column(BuyBaseField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Companion.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.comment) }
            }
        }
    return columns

}

internal val createButtonNew: @Composable (onCreate: () -> Unit) -> String = { onCreate ->
    IconButtonToolTip(
        "Добавить",
        onClick = onCreate,
        icon = Icons.Default.AddCircle,
        tint = MaterialTheme.colorScheme.primary
    )
    ""
}


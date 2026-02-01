@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.transaction.internal.component.tabs.component.buy

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.DateRow
import ru.pavlig43.coreui.NameRowWithSearchIcon
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.mutable.api.ui.DecimalFormat
import ru.pavlig43.mutable.api.ui.decimalColumn
import ru.pavlig43.mutable.api.ui.idWithSelection
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableCellTextField
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

enum class BuyField {
    SELECTION,
    COMPOSE_ID,
    COUNT,
    PRODUCT_NAME,
    DECLARATION_NAME,
    VENDOR_NAME,
    DATE_BORN,
    PRICE,
    COMMENT
}


@Suppress("LongMethod")
internal fun createBuyColumn(
    onOpenProductDialog: (Int) -> Unit,
    onOpenDeclarationDialog: (Int, Int) -> Unit,
    isChangeVisibleDialog: (Int) -> Unit,
    onEvent: (MutableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<BuyUi, BuyField, TableData<BuyUi>>> {
    val columns =
        editableTableColumns<BuyUi, BuyField, TableData<BuyUi>> {


            idWithSelection(
                selectionKey = BuyField.SELECTION,
                idKey = BuyField.COMPOSE_ID,
                onEvent = onEvent
            )


            column(BuyField.PRODUCT_NAME, valueOf = { it.productName }) {
                header("Продукт")
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
            decimalColumn(
                key = BuyField.COUNT,
                getValue = { it.count },
                headerText = "Количество",
                decimalFormat = DecimalFormat.KG(),
                onEvent = { updateEvent -> onEvent(updateEvent) },
                updateItem = { item, count -> item.copy(count = count) },
                footerValue = {tableData -> tableData.displayedItems.sumOf { it.count }}
            )
            decimalColumn(
                key = BuyField.PRICE,
                getValue = { it.price },
                headerText = "Цена",
                decimalFormat = DecimalFormat.RUB(),
                onEvent = { updateEvent -> onEvent(updateEvent) },
                updateItem = { item, newPrice -> item.copy(price = newPrice) },
                footerValue = {tableData -> tableData.displayedItems.sumOf { it.price }}
            )



            column(BuyField.DECLARATION_NAME, valueOf = { it.declarationName }) {
                header("Декларация")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ ->
                    NameRowWithSearchIcon(
                        text = item.declarationName,
                        onOpenChooseDialog = {
                            onOpenDeclarationDialog(
                                item.composeId,
                                item.productId
                            )
                        }
                    )
                }
                sortable()
            }
            column(BuyField.VENDOR_NAME, valueOf = { it.vendorName }) {
                header("Поставщик")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.vendorName) }
                sortable()
            }
            column(BuyField.DATE_BORN, { it.dateBorn }) {
                header("Дата производства")
                align(Alignment.Center)
                cell { item, _ ->
                    DateRow(
                        date = item.dateBorn,
                        isChangeDialogVisible = {isChangeVisibleDialog(item.composeId)}
                    )
                }
                sortable()
            }



            column(BuyField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.comment) }
                editCell {item,_,_->
                    TableCellTextField(
                        value = item.comment,
                        onValueChange = {onEvent(MutableUiEvent.UpdateItem(item.copy(comment = it)))},

                    )
                }
            }
        }
    return columns

}




package ru.pavlig43.transaction.internal.component.tabs.component.buy

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.coreui.NameRowWithSearchIcon
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.mutable.api.ui.DecimalFormat
import ru.pavlig43.mutable.api.ui.cellForDecimalFormat
import ru.pavlig43.mutable.api.ui.idWithSelection
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
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
    onOpenDeclarationDialog:(Int, Int)-> Unit,
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
            column(BuyField.COUNT, valueOf = { it.count }) {
                header("Количество")
                align(Alignment.Center)
                filter(
                    TableFilterType.NumberTableFilter(
                        delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    )
                )
                cellForDecimalFormat(
                    format = DecimalFormat.KG(),
                    getCount = { it.count },
                    saveInModel = { item, count ->
                        onEvent(MutableUiEvent.UpdateItem(item.copy(count = count)))
                    }
                )
                sortable()
            }
            column(BuyField.PRICE, valueOf = { it.price }) {

                header("Цена")
                align(Alignment.Center)
                filter(
                    TableFilterType.NumberTableFilter(
                        delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    )
                )
                cellForDecimalFormat(
                    format = DecimalFormat.RUB(),
                    getCount = { it.price },
                    saveInModel = { buyUi, newPrice ->
                        onEvent(MutableUiEvent.UpdateItem(buyUi.copy(price = newPrice)))
                    }
                )
                sortable()
            }

            column(BuyField.DECLARATION_NAME, valueOf = { it.declarationName }) {
                header("Декларация")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ ->
                    NameRowWithSearchIcon(
                        text = item.declarationName,
                        onOpenChooseDialog = { onOpenDeclarationDialog(item.composeId,item.productId) }
                    )
                }
                sortable()
            }
            column(BuyField.VENDOR_NAME, valueOf = { it.vendorName }) {
                header("Поставщик")
                align(Alignment.Companion.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.vendorName) }
                sortable()
            }

            column(BuyField.DATE_BORN, valueOf = { it.dateBorn }) {
                header("Дата производства")
                align(Alignment.Center)
                filter(TableFilterType.DateTableFilter())
                cell { row, _ ->
                    Text(row.dateBorn.format(dateFormat))
                }
                sortable()
            }

            column(BuyField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Companion.Center)
                filter(TableFilterType.TextTableFilter())
                cell { row, _ -> Text(row.comment) }
            }
        }
    return columns

}




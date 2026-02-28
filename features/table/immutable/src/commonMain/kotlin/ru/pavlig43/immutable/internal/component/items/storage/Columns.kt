package ru.pavlig43.immutable.internal.component.items.storage

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.immutable.internal.column.DecimalFormat
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.arrow_upward
import ua.wwind.table.ColumnSpec
import ua.wwind.table.tableColumns

enum class StorageProductField {
    EXPAND,
    PRODUCT_NAME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}
fun createStorageColumns(
    onToggleExpand: (productId: Int) -> Unit
): ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, TableData<StorageProductUi>>> =
    tableColumns {

        column(StorageProductField.EXPAND, valueOf = { it.expanded }) {
            title { "Партии" }
            resizable(false)
            cell { item, _ ->
                IconButton(
                    onClick = { onToggleExpand(item.productId) },
                ) {
                    if (item.expanded) {
                        Icon(
                            painterResource(Res.drawable.arrow_upward),
                            contentDescription = "Свернуть партии",
                        )
                    } else {
                        Icon(
                            painterResource(Res.drawable.arrow_downward),
                            contentDescription = "Развернуть партии",
                        )
                    }
                }
            }
        }

        readTextColumn(
            column = StorageProductField.PRODUCT_NAME,
            valueOf = { it.productName },
            headerText = "Продукт"
        )
        readDecimalColumn(
            headerText = "Старт",
            column = StorageProductField.BALANCE_BEFORE,
            valueOf = {it.balanceBeforeStart},
            decimalFormat = DecimalFormat.Decimal3(),
        )
        readDecimalColumn(
            headerText = "Приход",
            column = StorageProductField.INCOMING,
            valueOf = {it.incoming},
            decimalFormat = DecimalFormat.Decimal3(),
        )
        readDecimalColumn(
            headerText = "Расход",
            column = StorageProductField.OUTGOING,
            valueOf = {it.outgoing},
            decimalFormat = DecimalFormat.Decimal3(),
        )
        readDecimalColumn(
            headerText = "Остаток",
            column = StorageProductField.BALANCE_END,
            valueOf = {it.balanceOnEnd},
            decimalFormat = DecimalFormat.Decimal3(),
        )

    }
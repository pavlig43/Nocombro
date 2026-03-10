@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.safety

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.DecimalFormat
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class SafetyField {
    ID,
    PRODUCT_NAME,
    VENDOR_NAME,
    COUNT,
    REORDER_POINT,
    ORDER_QUANTITY
}

internal fun createSafetyColumn(
): ImmutableList<ColumnSpec<SafetyTableUi, SafetyField, TableData<SafetyTableUi>>> {
    val columns =
        tableColumns<SafetyTableUi, SafetyField, TableData<SafetyTableUi>> {

            readTextColumn(
                headerText = "Ид",
                column = SafetyField.ID,
                valueOf = { it.composeId.toString() }
            )

            readTextColumn(
                headerText = "Продукт",
                column = SafetyField.PRODUCT_NAME,
                valueOf = { it.productName },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Поставщик",
                column = SafetyField.VENDOR_NAME,
                valueOf = { it.vendorName },
                filterType = TableFilterType.TextTableFilter()
            )

            readDecimalColumn(
                headerText = "Остаток",
                column = SafetyField.COUNT,
                valueOf = { it.count },
                decimalFormat = DecimalFormat.Decimal3()
            )

            readDecimalColumn(
                headerText = "Точка заказа",
                column = SafetyField.REORDER_POINT,
                valueOf = { it.reorderPoint },
                decimalFormat = DecimalFormat.Decimal3()
            )

            readDecimalColumn(
                headerText = "Заказать",
                column = SafetyField.ORDER_QUANTITY,
                valueOf = { it.orderQuantity },
                decimalFormat = DecimalFormat.Decimal3()
            )
        }
    return columns
}

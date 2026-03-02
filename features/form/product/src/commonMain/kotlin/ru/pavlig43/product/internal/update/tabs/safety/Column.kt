@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.product.internal.update.tabs.safety

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

internal enum class SafetyStockField {
    /** Точка перезаказа */
    REORDER_POINT,
    /** Количество для заказа */
    ORDER_QUANTITY,
}
internal fun createSafetyStockColumns(
    onChangeItem: ((SafetyStockUi) -> SafetyStockUi) -> Unit
): ImmutableList<ColumnSpec<SafetyStockUi, SafetyStockField, Unit>> {
    val columns =
        editableTableColumns<SafetyStockUi, SafetyStockField, Unit> {

            decimalColumn(
                key = SafetyStockField.REORDER_POINT,
                getValue = { it.reorderPoint },
                headerText = "Точка перезаказа",
                decimalFormat = DecimalFormat.Decimal3(),
                updateItem = { item, newValue ->
                    onChangeItem { it.copy(reorderPoint = newValue) }
                },
                isSortable = false
            )

            // Количество для заказа
            decimalColumn(
                key = SafetyStockField.ORDER_QUANTITY,
                getValue = { it.orderQuantity },
                headerText = "Количество для заказа",
                decimalFormat = DecimalFormat.Decimal3(),
                updateItem = { item, newValue ->
                    onChangeItem { it.copy(orderQuantity = newValue) }
                },
                isSortable = false
            )
        }

    return columns
}

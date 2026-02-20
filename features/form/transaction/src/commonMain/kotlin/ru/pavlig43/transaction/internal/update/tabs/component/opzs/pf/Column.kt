@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf


import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.readTextColumn
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

internal enum class PfField {
    PRODUCT,
    DECLARATION,
    VENDOR,
    COUNT
}

@Suppress("LongMethod")
internal fun createPfColumns(
    onOpenProductDialog: () -> Unit,
    onOpenDeclarationDialog: () -> Unit,
    onChangeItem: (PfUi) -> Unit,
): ImmutableList<ColumnSpec<PfUi, PfField, Unit>> {
    val columns =
        editableTableColumns<PfUi, PfField, Unit> {
            textWithSearchIconColumn(
                headerText = "Продукт",
                column = PfField.PRODUCT,
                valueOf = { item -> item.productName },
                onOpenDialog = { onOpenProductDialog() },
            )

            textWithSearchIconColumn(
                headerText = "Декларация",
                column = PfField.DECLARATION,
                valueOf = { item -> item.declarationName },
                onOpenDialog = {
                    if (it.productId != 0) {
                        onOpenDeclarationDialog()
                    }
                },
            )

            readTextColumn(
                headerText = "Поставщик",
                column = PfField.VENDOR,
                valueOf = { item -> item.vendorName },
            )

            decimalColumn(
                key = PfField.COUNT,
                getValue = { item -> item.count },
                headerText = "Количество",
                decimalFormat = DecimalFormat.Decimal3(),
                updateItem = { item, count -> onChangeItem(item.copy(count = count)) }
            )
        }
    return columns
}

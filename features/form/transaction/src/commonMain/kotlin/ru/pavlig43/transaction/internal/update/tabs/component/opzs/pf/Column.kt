@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

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
                filterType = TableFilterType.TextTableFilter()
            )

            textWithSearchIconColumn(
                headerText = "Декларация",
                column = PfField.DECLARATION,
                valueOf = { item -> item.declarationName },
                onOpenDialog = { onOpenDeclarationDialog() },
                filterType = TableFilterType.TextTableFilter()
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

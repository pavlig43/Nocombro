@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.product

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readDateColumn
import ru.pavlig43.immutable.internal.column.readEnumColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class ProductField {

    SELECTION,

    ID,
    NAME,
    TYPE,
    CREATED_AT,
    COMMENT
}
@Suppress("LongMethod")
internal fun createProductColumn(
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<ProductTableUi, ProductField, TableData<ProductTableUi>>> {
    val columns =
        tableColumns<ProductTableUi, ProductField, TableData<ProductTableUi>> {

            idWithSelection(
                selectionKey = ProductField.SELECTION,
                idKey = ProductField.ID,
                onEvent = onEvent
            )

            readTextColumn(
                headerText = "Название",
                column = ProductField.NAME,
                valueOf = { it.displayName },
                filterType = TableFilterType.TextTableFilter()
            )

            readEnumColumn(
                headerText = "Тип",
                column = ProductField.TYPE,
                valueOf = { it.type },
                filterType = TableFilterType.EnumTableFilter(
                    ProductType.entries.toImmutableList(),
                    getTitle = { it.displayName }
                ),
                getTitle = { it.displayName }
            )

            readDateColumn(
                headerText = "Создан",
                column = ProductField.CREATED_AT,
                valueOf = { it.createdAt },
                filterType = TableFilterType.DateTableFilter()
            )

            readTextColumn(
                headerText = "Комментарий",
                column = ProductField.COMMENT,
                valueOf = { it.comment },
                filterType = TableFilterType.TextTableFilter()
            )
        }
    return columns

}

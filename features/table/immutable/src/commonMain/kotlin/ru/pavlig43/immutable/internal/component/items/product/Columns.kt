@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.product

import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.ui.idWithSelection
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
    listTypeForFilter: List<ProductType>,
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<ProductTableUi, ProductField, TableData<ProductTableUi>>> {
    val columns =
        tableColumns<ProductTableUi, ProductField, TableData<ProductTableUi>> {

            idWithSelection(
                selectionKey = ProductField.SELECTION,
                idKey = ProductField.ID,
                onEvent = onEvent
            )

            column(ProductField.NAME, valueOf = { it.displayName }) {
                header("Название")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { document, _ -> Text(document.displayName) }
                sortable()
            }
            column(ProductField.TYPE, valueOf = { it.type }) {
                header("Тип")
                align(Alignment.Center)
                filter(
                    TableFilterType.EnumTableFilter(
                        listTypeForFilter.map { it.enumValue }.toImmutableList(),
                        getTitle = { it.displayName })
                )
                cell { document, _ -> Text(document.type.enumValue.displayName) }
            }
            column(ProductField.CREATED_AT, valueOf = { it.createdAt }) {
                header("Создан")
                align(Alignment.Center)
                filter(TableFilterType.DateTableFilter())
                cell { document, _ ->
                    Text(
                        document.createdAt.format(dateFormat)
                    )
                }
                sortable()
            }
            column(ProductField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { document, _ -> Text(document.comment) }
            }
        }
    return columns

}

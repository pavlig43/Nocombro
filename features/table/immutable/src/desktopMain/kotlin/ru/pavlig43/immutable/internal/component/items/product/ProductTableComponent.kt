package ru.pavlig43.immutable.internal.component.items.product

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList

import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.column.toTableDisplayText
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.component.displayedTextSearchMatcher
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class ProductTableComponent(
    componentContext: ComponentContext,
    tableBuilder: ProductImmutableTableBuilder,
    onItemClick: (ProductTableUi) -> Unit,
    onCreate: () -> Unit,
    repository: ImmutableListRepository<ProductTableItem>,
) : ImmutableTableComponent<ProductTableItem, ProductTableUi, ProductField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = ProductFilterMatcher,
    searchMatcher = ProductSearchMatcher,
    sortMatcher = ProductSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<ProductTableUi, ProductField, TableData<ProductTableUi>>> =
        createProductColumn(::onEvent)

    override val exportConfiguration: TableExportConfiguration<ProductTableUi, ProductField> =
        TableExportConfiguration(
            suggestedFileName = "products-export",
        )

}

internal val ProductSearchMatcher = displayedTextSearchMatcher<ProductTableUi> { item ->
    listOf(
        item.composeId.toString(),
        item.displayName,
        item.vendorNames,
        item.type.displayName,
        item.createdAt.toTableDisplayText(),
        item.secondName,
        item.comment,
    )
}

private fun ProductTableItem.toUi(): ProductTableUi {
    return ProductTableUi(
        composeId = product.id,
        displayName = product.displayName,
        secondName = product.secondName,
        vendorNames = vendorNames,
        type = product.type,
        createdAt = product.createdAt,
        comment = product.comment,
        priceForSale = product.priceForSale,
        recNds = product.recNds
    )
}

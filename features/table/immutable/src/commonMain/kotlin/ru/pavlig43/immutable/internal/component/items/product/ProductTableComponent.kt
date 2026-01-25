package ru.pavlig43.immutable.internal.component.items.product

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class ProductTableComponent(
    componentContext: ComponentContext,
    tableBuilder: ProductImmutableTableBuilder,
    onCreate: () -> Unit,
    onItemClick: (ProductTableUi) -> Unit,
    repository: ImmutableListRepository<Product>,
) : ImmutableTableComponent<Product, ProductTableUi, ProductField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = ProductFilterMatcher,
    sortMatcher = ProductSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<ProductTableUi, ProductField, TableData<ProductTableUi>>> =
        createProductColumn(tableBuilder.fullListProductTypes,::onEvent)

}
private fun Product.toUi(): ProductTableUi {
    return ProductTableUi(
        composeId = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}
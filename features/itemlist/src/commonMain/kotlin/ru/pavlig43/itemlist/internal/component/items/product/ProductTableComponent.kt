package ru.pavlig43.itemlist.internal.component.items.product

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.itemlist.api.component.ProductBuilder
import ru.pavlig43.itemlist.internal.component.ImmutableTableComponent
import ru.pavlig43.itemlist.internal.data.ImmutableListRepository
import ru.pavlig43.itemlist.internal.model.TableData
import ua.wwind.table.ColumnSpec

internal class ProductTableComponent(
    componentContext: ComponentContext,
    tableBuilder: ProductBuilder,
    onCreate: () -> Unit,
    onItemClick: (ProductItemUi) -> Unit,
    repository: ImmutableListRepository<Product>,
) : ImmutableTableComponent<Product, ProductItemUi, ProductField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = ProductFilterMatcher,
    sortMatcher = ProductSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<ProductItemUi, ProductField, TableData<ProductItemUi>>> =
        createProductColumn(onCreate,tableBuilder.fullListProductTypes,::onEvent)

}
private fun Product.toUi(): ProductItemUi {
    return ProductItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}
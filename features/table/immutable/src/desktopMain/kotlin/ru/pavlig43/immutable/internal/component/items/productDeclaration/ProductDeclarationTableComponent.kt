package ru.pavlig43.immutable.internal.component.items.productDeclaration

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.immutable.api.component.ProductDeclarationImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec


internal class ProductDeclarationTableComponent(
    componentContext: ComponentContext,
    tableBuilder: ProductDeclarationImmutableTableBuilder,
    onCreate: () -> Unit,
    onItemClick: (ProductDeclarationTableUi) -> Unit,
    repository: ImmutableListRepository<ProductDeclarationOut>,
) : ImmutableTableComponent<ProductDeclarationOut, ProductDeclarationTableUi, ProductDeclarationField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = ProductDeclarationFilterMatcher,
    sortMatcher = ProductDeclarationSorter,
    repository = repository,
) {
    @Suppress("MaxLineLength")
    override val columns: ImmutableList<ColumnSpec<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>>> =
        createProductDeclarationColumn(
            onEvent = ::onEvent
        )


}

private fun ProductDeclarationOut.toUi(): ProductDeclarationTableUi {
    return ProductDeclarationTableUi(
        composeId = id,
        declarationId = declarationId,
        productId = productId,
        vendorName = vendorName,
        displayName = declarationName,
        isActual = isActual
    )
}

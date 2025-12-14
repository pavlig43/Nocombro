package ru.pavlig43.itemlist.statik.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.core.component.ValueFilterComponent
import ru.pavlig43.itemlist.statik.api.ProductListParamProvider

internal class ProductStaticListContainer(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    paramProvider: ProductListParamProvider,
    private val productListRepository: ProductListRepository,

    ) : ComponentContext by componentContext, IStaticListContainer<Product, ProductItemUi> {



    val typeFilterComponent = ValueFilterComponent(
        componentContext = childContext("types"),
        initialValue = paramProvider.fullListProductTypes
    )

    val searchTextFilterComponent = ValueFilterComponent(
        componentContext = childContext("search_text"),
        initialValue = ""
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val productListFlow: Flow<RequestResult<List<Product>>> = combine(
        typeFilterComponent.valueFlow,
        searchTextFilterComponent.valueFlow
    ) { types, text ->
        productListRepository.observeOnItems(
            searchText = text,
            types = types
        )
    }.flatMapLatest { it }



    override val staticListComponent: StaticListComponent<Product, ProductItemUi> =
        StaticListComponent(
            componentContext = childContext("body"),
            dataFlow = productListFlow,
            deleteItemsById = productListRepository::deleteByIds,
            searchTextComponent = searchTextFilterComponent,
            onCreate = onCreate,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Product::toUi,
        )


}
data class ProductItemUi(
    override val id: Int,
    val displayName: String,
    val type: ProductType,
    val createdAt: Long,
    val comment: String = "",
) : IItemUi

private fun Product.toUi(): ProductItemUi {
    return ProductItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}
internal class ProductListRepository(
    db: NocombroDatabase
) {
    private val dao = db.productDao
    private val tag = "ProductListRepository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteProductsByIds(ids)
        }
    }

    fun observeOnItems(
        searchText: String,
        types: List<ProductType>,
    ): Flow<RequestResult<List<Product>>> {
        return dbSafeFlow(tag) {
            dao.observeOnProducts(
                searchText = searchText,
                types = types
            )
        }
    }
}
package ru.pavlig43.itemlist.internal.component

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
import ru.pavlig43.itemlist.api.ProductListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.generateComponent

internal class ProductListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    paramProvider: ProductListParamProvider,
    private val productListRepository: ProductListRepository,

    ) : ComponentContext by componentContext, IListComponent<Product, ProductItemUi> {



    val typeComponent = generateComponent(ItemFilter.Type(paramProvider.fullListProductTypes))

    val searchTextComponent = generateComponent(ItemFilter.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val productListFlow: Flow<RequestResult<List<Product>>> = combine(
        typeComponent.filterFlow,
        searchTextComponent.filterFlow
    ) { types: ItemFilter.Type<ProductType>, text: ItemFilter.SearchText ->
        productListRepository.observeOnItems(
            searchText = text.value,
            types = types.value
        )
    }.flatMapLatest { it }



    override val staticItemsBodyComponent: StaticItemsBodyComponent<Product, ProductItemUi> =
        StaticItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = productListFlow,
            deleteItemsById = productListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
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
package ru.pavlig43.itemlist.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.RequestResult
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.component.ProductListParamProvider
import ru.pavlig43.itemlist.api.data.ProductListRepository
import ru.pavlig43.itemlist.internal.ItemFilter1
import ru.pavlig43.itemlist.internal.generateComponent

internal class ProductListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    paramProvider: ProductListParamProvider,
    private val productListRepository: ProductListRepository,

    ) : ComponentContext by componentContext, IListComponent<Product, ProductItemUi> {



    val typeComponent = generateComponent(ItemFilter1.Type(paramProvider.fullListProductTypes))

    val searchTextComponent = generateComponent(ItemFilter1.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val productListFlow: Flow<RequestResult<List<Product>>> = combine(
        typeComponent.filterFlow,
        searchTextComponent.filterFlow
    ) { types: ItemFilter1.Type<ProductType>, text: ItemFilter1.SearchText ->
        productListRepository.observeOnItems(
            searchText = text.value,
            types = types.value
        )
    }.flatMapLatest { it }



    override val itemsBodyComponent: ItemsBodyComponent<Product, ProductItemUi> =
        ItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = productListFlow,
            deleteItemsById = productListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Product::toUi,
        )


}
internal data class ProductItemUi(
    override val id: Int,
    override val displayName: String,
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
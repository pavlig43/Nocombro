package ru.pavlig43.itemlist.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.RequestResult
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.itemlist.api.component.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.component.VendorListParamProvider
import ru.pavlig43.itemlist.api.data.DeclarationListRepository
import ru.pavlig43.itemlist.api.data.VendorListRepository
import ru.pavlig43.itemlist.internal.ItemFilter1
import ru.pavlig43.itemlist.internal.generateComponent


internal class VendorListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    private val vendorListRepository: VendorListRepository,
    paramProvider: VendorListParamProvider
) : ComponentContext by componentContext, IListComponent<Vendor, VendorItemUi> {

    val searchTextComponent = generateComponent(ItemFilter1.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val vendorListFlow: Flow<RequestResult<List<Vendor>>> =
        searchTextComponent.filterFlow
            .flatMapLatest { text: ItemFilter1.SearchText ->
                vendorListRepository.observeVendorByFilter(text.value)
            }


    override val itemsBodyComponent =
        ItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = vendorListFlow,
            deleteItemsById = vendorListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Vendor::toUi,
        )


}
internal data class VendorItemUi(
    override val displayName: String,
    val comment: String,
    override val id: Int = 0,
): IItemUi

private fun Vendor.toUi(): VendorItemUi {
    return VendorItemUi(
        id = id,
        displayName = displayName,
        comment = comment
        )
}



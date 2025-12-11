package ru.pavlig43.itemlist.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.itemlist.api.VendorListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.generateComponent


internal class VendorListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    private val vendorListRepository: VendorListRepository,
    paramProvider: VendorListParamProvider
) : ComponentContext by componentContext, IListComponent<Vendor, VendorItemUi> {

    val searchTextComponent = generateComponent(ItemFilter.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val vendorListFlow: Flow<RequestResult<List<Vendor>>> =
        searchTextComponent.filterFlow
            .flatMapLatest { text: ItemFilter.SearchText ->
                vendorListRepository.observeVendorByFilter(text.value)
            }


    override val staticItemsBodyComponent =
        StaticItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = vendorListFlow,
            deleteItemsById = vendorListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Vendor::toUi,
        )


}
data class VendorItemUi(
    val displayName: String,
    val comment: String,
    override val id: Int = 0,
): IItemUi{
    override val composeKey: Int = id
}

private fun Vendor.toUi(): VendorItemUi {
    return VendorItemUi(
        id = id,
        displayName = displayName,
        comment = comment
        )
}
internal class VendorListRepository(
    db: NocombroDatabase
) {
    private val dao = db.vendorDao
    private val tag = "Vendor list repository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteVendorsByIds(ids)
        }
    }


    fun observeVendorByFilter(
        text: String
    ): Flow<RequestResult<List<Vendor>>> {

        return dbSafeFlow(tag) { dao.observeOnVendors(text) }
    }
}



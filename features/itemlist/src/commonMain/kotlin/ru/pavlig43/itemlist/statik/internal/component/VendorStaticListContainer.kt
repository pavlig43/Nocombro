package ru.pavlig43.itemlist.statik.internal.component

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
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ru.pavlig43.itemlist.core.component.ValueFilterComponent
import ru.pavlig43.itemlist.core.refac.api.VendorListParamProvider


internal class VendorStaticListContainer(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    private val vendorListRepository: VendorListRepository,
    paramProvider: VendorListParamProvider
) : ComponentContext by componentContext, IStaticListContainer<Vendor, VendorItemUi> {

    val searchTextFilterComponent = ValueFilterComponent(
        componentContext = childContext("search_text"),
        initialValue = ""
    )


    @OptIn(ExperimentalCoroutinesApi::class)
    private val vendorListFlow: Flow<RequestResult<List<Vendor>>> =
       searchTextFilterComponent.valueFlow
            .flatMapLatest { text->
                vendorListRepository.observeVendorByFilter(text)
            }


    override val staticListComponent =
        StaticListComponent(
            componentContext = childContext("body"),
            dataFlow = vendorListFlow,
            deleteItemsById = vendorListRepository::deleteByIds,
            searchTextComponent = searchTextFilterComponent,
            onCreate = onCreate,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Vendor::toUi,
        )


}
data class VendorItemUi(
    val displayName: String,
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



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
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.itemlist.api.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.generateComponent

internal class DeclarationListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    private val declarationListRepository: DeclarationListRepository,
    paramProvider: DeclarationListParamProvider
) : ComponentContext by componentContext, IListComponent<Declaration, DeclarationItemUi> {

    val searchTextComponent = generateComponent(ItemFilter.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val declarationListFlow: Flow<RequestResult<List<Declaration>>> =
        searchTextComponent.filterFlow
            .flatMapLatest { text: ItemFilter.SearchText ->
                declarationListRepository.observeDeclarationByFilter(text.value)
            }


    override val itemsBodyComponent =
        ItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = declarationListFlow,
            deleteItemsById = declarationListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Declaration::toUi,
        )


}
data class DeclarationItemUi(

    override val displayName: String,

    val createdAt: Long,

    val vendorId: Int,

    val vendorName: String,

    val bestBefore: Long,

    override val id: Int = 0,
): IItemUi

private fun Declaration.toUi(): DeclarationItemUi {
    return DeclarationItemUi(
        id = id,
        vendorId = vendorId,
        vendorName = vendorName,
        bestBefore = bestBefore,
        displayName = displayName,
        createdAt = createdAt,

        )

}
internal class DeclarationListRepository(
    db: NocombroDatabase
) {
    private val dao = db.declarationDao
    private val tag = "Declaration  list repository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteDeclarationsByIds(ids)
        }
    }


    fun observeDeclarationByFilter(
        text: String
    ): Flow<RequestResult<List<Declaration>>> {

        return dbSafeFlow(tag) { dao.observeOnItems(text, text.isNotBlank()) }
    }
}



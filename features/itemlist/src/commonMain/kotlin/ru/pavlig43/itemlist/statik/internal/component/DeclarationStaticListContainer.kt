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
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ru.pavlig43.itemlist.core.component.ValueFilterComponent
import ru.pavlig43.itemlist.core.refac.api.DeclarationListParamProvider

internal class DeclarationStaticListContainer(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    private val declarationListRepository: DeclarationListRepository,
    paramProvider: DeclarationListParamProvider
) : ComponentContext by componentContext, IStaticListContainer<Declaration, DeclarationItemUi> {

    val searchTextFilterComponent = ValueFilterComponent(
        componentContext = childContext("search_text"),
        initialValue = ""
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val declarationListFlow: Flow<RequestResult<List<Declaration>>> =
        searchTextFilterComponent.valueFlow
            .flatMapLatest { text ->
                declarationListRepository.observeDeclarationByFilter(text)
            }


    override val staticListComponent =
        StaticListComponent(
            componentContext = childContext("body"),
            dataFlow = declarationListFlow,
            deleteItemsById = declarationListRepository::deleteByIds,
            searchTextComponent = searchTextFilterComponent,
            onCreate = onCreate,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Declaration::toUi,
        )


}
data class DeclarationItemUi(

    val displayName: String="",

    val createdAt: Long =0,

    val vendorId: Int = 0,

    val vendorName: String = "",

    val bestBefore: Long = 0,

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



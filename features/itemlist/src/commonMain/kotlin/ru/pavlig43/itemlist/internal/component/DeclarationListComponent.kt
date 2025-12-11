package ru.pavlig43.itemlist.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.DateFieldKind
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.convertToDateOrDateTimeString
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.itemlist.api.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.generateComponent
import ru.pavlig43.itemlist.internal.ui.CREATED_AT
import ru.pavlig43.itemlist.internal.ui.CREATED_AT_WIDTH
import ru.pavlig43.itemlist.internal.ui.ID
import ru.pavlig43.itemlist.internal.ui.ID_WIDTH
import ru.pavlig43.itemlist.internal.ui.NAME
import ru.pavlig43.itemlist.internal.ui.NAME_WIDTH
import ru.pavlig43.itemlist.internal.ui.core.ColumnDefinition1
import ru.pavlig43.itemlist.internal.ui.core.TextCellElement
import ru.pavlig43.itemlist.internal.ui.core.TextFieldCellElement

internal class DeclarationListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    private val declarationListRepository: DeclarationListRepository,
    paramProvider: DeclarationListParamProvider
) : ComponentContext by componentContext, IListComponent<Declaration, DeclarationItemUi> {

    val declarationColumns = listOf<ColumnDefinition1<DeclarationItemUi>>(
        ColumnDefinition1(
            width = ID_WIDTH,
            headerTitle = ID,
            cellProvider = { TextCellElement(it.id.toString(), ID_WIDTH) }
        ),
        ColumnDefinition1(
            width = NAME_WIDTH,
            headerTitle = NAME,
            cellProvider = { TextCellElement(it.displayName, NAME_WIDTH) }
        ),
        ColumnDefinition1(
            width = NAME_WIDTH,
            headerTitle = "Поставщик",
            cellProvider = { TextCellElement(it.vendorName, NAME_WIDTH) }
        ),
        ColumnDefinition1(
            width = CREATED_AT_WIDTH,
            headerTitle = CREATED_AT,
            cellProvider = {
                TextCellElement(
                    it.createdAt.convertToDateOrDateTimeString(DateFieldKind.Date),
                    CREATED_AT_WIDTH,

                )
            }
        ),
        ColumnDefinition1(
            width = CREATED_AT_WIDTH,
            headerTitle = "Годен до",
            cellProvider = {
                TextCellElement(
                    it.bestBefore.convertToDateOrDateTimeString(DateFieldKind.Date),
                    CREATED_AT_WIDTH
                )
            }
        ),
    )

    val searchTextComponent = generateComponent(ItemFilter.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val declarationListFlow: Flow<RequestResult<List<Declaration>>> =
        searchTextComponent.filterFlow
            .flatMapLatest { text: ItemFilter.SearchText ->
                declarationListRepository.observeDeclarationByFilter(text.value)
            }
    val declarationListFlow1 = MutableStateFlow(listOf(
        DeclarationItemUi(
        )
    ))



    override val staticItemsBodyComponent =
        StaticItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = declarationListFlow,
            deleteItemsById = declarationListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Declaration::toUi,
        )
    val staticItemsBodyComponent1 =
        StaticItemsBodyComponent1(
            componentContext = childContext("body1"),
            dataFlow = declarationListFlow,
            deleteItemsById = declarationListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Declaration::toUi,
            columnDefinition = declarationColumns,
        )



}
data class DeclarationItemUi(

    val displayName: String="",

    val createdAt: Long =0,

    val vendorId: Int = 0,

    val vendorName: String = "",

    val bestBefore: Long = 0,

    override val id: Int = 0,
): IItemUi{
    override val composeKey: Int = id
}

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



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
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.DocumentListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.generateComponent

internal class DocumentsListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    paramProvider: DocumentListParamProvider,
    private val documentListRepository: DocumentListRepository,

    ) : ComponentContext by componentContext, IListComponent<Document, DocumentItemUi> {



    val typeComponent = generateComponent(ItemFilter.Type(paramProvider.fullListDocumentTypes))

    val searchTextComponent = generateComponent(ItemFilter.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val documentListFlow: Flow<RequestResult<List<Document>>> = combine(
        typeComponent.filterFlow,
        searchTextComponent.filterFlow
    ) { types: ItemFilter.Type<DocumentType>, text: ItemFilter.SearchText ->
        documentListRepository.observeOnItems(
            searchText = text.value,
            types = types.value
        )
    }.flatMapLatest { it }



    override val itemsBodyComponent: ItemsBodyComponent<Document, DocumentItemUi> =
        ItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = documentListFlow,
            deleteItemsById = documentListRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Document::toUi,
        )


}
internal data class DocumentItemUi(
    override val id: Int,
    override val displayName: String,
    val type: DocumentType,
    val createdAt: Long,
    val comment: String = "",
) : IItemUi

private fun Document.toUi(): DocumentItemUi {
    return DocumentItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}
internal class DocumentListRepository(
    db: NocombroDatabase
) {
    private val dao = db.documentDao
    private val tag = "DocumentListRepository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteDocumentsByIds(ids)
        }
    }

    fun observeOnItems(
        searchText: String,
        types: List<DocumentType>,
    ): Flow<RequestResult<List<Document>>> {
        return dbSafeFlow(tag) {
            dao.observeOnDocuments(
                searchText = searchText,
                types = types
            )
        }
    }
}
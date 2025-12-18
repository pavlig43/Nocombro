package ru.pavlig43.itemlist.statik.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.core.component.ValueFilterComponent
import ru.pavlig43.itemlist.statik.api.DocumentListParamProvider
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class DocumentsStaticListContainer(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (DocumentItemUi) -> Unit,
    paramProvider: DocumentListParamProvider,
    private val documentListRepository: DocumentListRepository,

    ) : ComponentContext by componentContext, IStaticListContainer<Document, DocumentItemUi> {


    val typeFilterComponent = ValueFilterComponent(
        componentContext = childContext("types"),
        initialValue = paramProvider.fullListDocumentTypes
    )

    val searchTextFilterComponent = ValueFilterComponent(
        componentContext = childContext("search_text"),
        initialValue = ""
    )



    @OptIn(ExperimentalCoroutinesApi::class)
    private val documentListFlow: Flow<RequestResult<List<Document>>> = combine(
        typeFilterComponent.valueFlow,
        searchTextFilterComponent.valueFlow
    ) { types, text ->
        documentListRepository.observeOnItems()
    }.flatMapLatest { it }


    override val staticListComponent: StaticListComponent<Document, DocumentItemUi> =
        StaticListComponent(
            componentContext = childContext("body"),
            dataFlow = documentListFlow,
            deleteItemsById = {ids-> documentListRepository.deleteByIds(ids.toSet()) },
            searchTextComponent = searchTextFilterComponent,
            onCreate = onCreate,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = Document::toUi,
        )


}

data class DocumentItemUi(
    override val id: Int,
    val displayName: String,
    val type: DocumentType,
    val createdAt: LocalDate,
    val comment: String = "",
) : IItemUi

@OptIn(ExperimentalTime::class)
private fun Document.toUi(): DocumentItemUi {
    val date = Instant.fromEpochMilliseconds(createdAt)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return DocumentItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = date,
        comment = comment
    )
}
internal class DocListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (DocumentItemUi) -> Unit,
    paramProvider: DocumentListParamProvider,
    private val documentListRepository: DocumentListRepository,
): ComponentContext by componentContext{

}

class DocumentListRepository(
    db: NocombroDatabase
) {
    private val dao = db.documentDao
    private val tag = "DocumentListRepository"

    suspend fun deleteByIds(ids: Set<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteDocumentsByIds(ids)
        }
    }

    fun observeOnItems(
    ): Flow<RequestResult<List<Document>>> {
        return dbSafeFlow(tag) {
            dao.observeOnDocuments(
            )
        }
    }
}
package ru.pavlig43.itemlist.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.RequestResult
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.internal.data.DocumentItemUi
import ru.pavlig43.itemlist.api.component.DocumentListParamProvider
import ru.pavlig43.itemlist.api.data.DocumentListRepository
import ru.pavlig43.itemlist.internal.ItemFilter1
import ru.pavlig43.itemlist.internal.generateComponent

internal class DocumentsListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    paramProvider: DocumentListParamProvider,
    private val documentListRepository: DocumentListRepository,

    ) : ComponentContext by componentContext, IListComponent<Document, DocumentItemUi> {



    val typeComponent = generateComponent(ItemFilter1.Type(paramProvider.fullListDocumentTypes))

    val searchTextComponent = generateComponent(ItemFilter1.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val documentListFlow: Flow<RequestResult<List<Document>>> = combine(
        typeComponent.filterFlow,
        searchTextComponent.filterFlow
    ) { types: ItemFilter1.Type<DocumentType>, text: ItemFilter1.SearchText ->
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

private fun Document.toUi(): DocumentItemUi {
    return DocumentItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}
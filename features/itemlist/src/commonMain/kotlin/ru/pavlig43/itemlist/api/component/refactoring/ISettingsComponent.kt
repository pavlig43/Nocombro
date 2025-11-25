package ru.pavlig43.itemlist.api.component.refactoring

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.internal.di.DocumentListRepository
import ru.pavlig43.itemlist.internal.di.createItemListFormModule

class ItemListFactoryComponent(
    componentContext: ComponentContext,
    itemListDependencies: IItemListDependencies,
    itemListParamProvider: ItemListParamProvider
): ComponentContext by componentContext{

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(
        createItemListFormModule(itemListDependencies)
    )
    internal val a: ISettingsComponent<out GenericItem, out IItemUi> = when(itemListParamProvider){
        is DocumentListParamProvider -> DocumentsListComponent(
            componentContext = componentContext,
            paramProvider = itemListParamProvider,
            documentListRepository = scope.get()
        )

        is DeclarationListParamProvider -> DeclarationListComponent(
            componentContext = componentContext,
            paramProvider = itemListParamProvider
        )
    }

}

sealed interface ItemListParamProvider
data class DocumentListParamProvider(
    val tabTitle: String,
    val fullListDocumentTypes: List<DocumentType>,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    val withCheckbox: Boolean,
): ItemListParamProvider

data class DeclarationListParamProvider(
    val tabTitle: String,
): ItemListParamProvider
internal class DeclarationListComponent(
    componentContext: ComponentContext,
    paramProvider: DeclarationListParamProvider
): ComponentContext by componentContext,ISettingsComponent<DeclarationIn, DocumentItemUi> {
    override val itemsBodyComponent: ItemsBodyComponent<DeclarationIn, DocumentItemUi>
        get() = TODO("Not yet implemented")
}

internal class DocumentsListComponent(
    componentContext: ComponentContext,
    paramProvider: DocumentListParamProvider,
    private val documentListRepository: DocumentListRepository,

) : ComponentContext by componentContext, ISettingsComponent<Document, DocumentItemUi>,
    SlotComponent {

    private val _model = MutableStateFlow(SlotComponent.TabModel(paramProvider.tabTitle))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    private val typeComponent = generateComponent(ItemFilter1.Type(paramProvider.fullListDocumentTypes))

    private val searchTextComponent = generateComponent(ItemFilter1.SearchText(""))

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
            onItemClick = paramProvider.onItemClick,
            mapper = Document::toUi,
            onCreate = paramProvider.onCreate
        )


}
private fun Document.toUi(): DocumentItemUi{
    return DocumentItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}


sealed class ItemFilter1(val componentName: String) {
    data class Type<I : ItemType>(val value: List<I>) : ItemFilter1("types")
    data class SearchText(val value: String) : ItemFilter1("search_text")
}


fun <T : ItemFilter1> ComponentContext.generateComponent(filter: T): BaseFilterComponent<T> {
    val context = childContext(filter.componentName)
    return BaseFilterComponent(context, filter)
}


class BaseFilterComponent<T : ItemFilter1>(
    componentContext: ComponentContext,
    initialValue: T
) : ComponentContext by componentContext {

    private val _filterFlow = MutableStateFlow(initialValue)
    val filterFlow: StateFlow<T> = _filterFlow.asStateFlow()

    fun onChangeFilter(new: T) {
        _filterFlow.update { new }
    }

}
data class DocumentItemUi(
    override val id: Int,
    override val displayName: String,
    val type: DocumentType,
    val createdAt: Long,
    val comment: String = "",
) : IItemUi

sealed interface ISettingsComponent<O : GenericItem, U : IItemUi> {
    val itemsBodyComponent: ItemsBodyComponent<O, U>
}



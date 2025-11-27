package ru.pavlig43.itemlist.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.RequestResult
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.itemlist.api.component.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.component.DocumentListParamProvider
import ru.pavlig43.itemlist.api.data.DeclarationListRepository
import ru.pavlig43.itemlist.api.data.DocumentListRepository
import ru.pavlig43.itemlist.internal.ItemFilter1
import ru.pavlig43.itemlist.internal.data.DeclarationItemUi
import ru.pavlig43.itemlist.internal.data.DocumentItemUi
import ru.pavlig43.itemlist.internal.generateComponent

internal class DeclarationListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    private val declarationListRepository: DeclarationListRepository,
    paramProvider: DeclarationListParamProvider
) : ComponentContext by componentContext, IListComponent<DeclarationIn, DeclarationItemUi> {

    val searchTextComponent = generateComponent(ItemFilter1.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val declarationListFlow: Flow<RequestResult<List<DeclarationIn>>> =
        searchTextComponent.filterFlow
            .flatMapLatest { text: ItemFilter1.SearchText ->
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
            mapper = DeclarationIn::toUi,
        )


}

private fun DeclarationIn.toUi(): DeclarationItemUi {
    return DeclarationItemUi(
        id = id,
        vendorId = vendorId,
        vendorName = vendorName,
        bestBefore = bestBefore,
        displayName = displayName,
        createdAt = createdAt,

        )

}


package ru.pavlig43.itemlist.api.component.refactoring

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.scope.Scope
import org.koin.core.scope.get
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.itemlist.api.component.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.component.DocumentListParamProvider
import ru.pavlig43.itemlist.api.component.ItemListParamProvider
import ru.pavlig43.itemlist.internal.component.DeclarationListComponent
import ru.pavlig43.itemlist.internal.component.DocumentsListComponent
import ru.pavlig43.itemlist.internal.component.IListComponent

class ItemListFactoryComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    scope: Scope,
    itemListParamProvider: ItemListParamProvider
): ComponentContext by componentContext, SlotComponent {
    private val _model = MutableStateFlow(SlotComponent.TabModel("Изменить"))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    internal val listComponent: IListComponent<out GenericItem, out IItemUi> = when(itemListParamProvider){
        is DocumentListParamProvider -> DocumentsListComponent(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            paramProvider = itemListParamProvider,
            documentListRepository = scope.get()
        )

        is DeclarationListParamProvider -> DeclarationListComponent(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            declarationListRepository = scope.get(),
            paramProvider = itemListParamProvider
        )
    }

}
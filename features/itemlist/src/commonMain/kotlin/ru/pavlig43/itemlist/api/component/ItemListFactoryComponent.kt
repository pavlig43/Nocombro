package ru.pavlig43.itemlist.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.itemlist.api.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.DocumentListParamProvider
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.itemlist.api.ItemListParamProvider
import ru.pavlig43.itemlist.api.ProductListParamProvider
import ru.pavlig43.itemlist.api.VendorListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.component.DeclarationListComponent
import ru.pavlig43.itemlist.internal.component.DocumentsListComponent
import ru.pavlig43.itemlist.internal.component.IListComponent
import ru.pavlig43.itemlist.internal.component.ProductListComponent
import ru.pavlig43.itemlist.internal.component.VendorListComponent
import ru.pavlig43.itemlist.internal.di.moduleFactory

class ItemListFactoryComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    itemListDependencies: ItemListDependencies,
    itemListParamProvider: ItemListParamProvider
): ComponentContext by componentContext, SlotComponent {

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(moduleFactory(itemListDependencies))
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

        is ProductListParamProvider -> ProductListComponent(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            paramProvider = itemListParamProvider,
            productListRepository = scope.get()
        )

        is VendorListParamProvider -> VendorListComponent(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            vendorListRepository = scope.get(),
            paramProvider = itemListParamProvider
        )
    }

}


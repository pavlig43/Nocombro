package ru.pavlig43.itemlist.statik.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.statik.api.DeclarationListParamProvider
import ru.pavlig43.itemlist.statik.api.DocumentListParamProvider
import ru.pavlig43.itemlist.statik.api.ItemListParamProvider
import ru.pavlig43.itemlist.statik.api.ProductListParamProvider
import ru.pavlig43.itemlist.statik.api.TransactionListParamProvider
import ru.pavlig43.itemlist.statik.api.VendorListParamProvider
import ru.pavlig43.itemlist.statik.internal.component.DeclarationStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.DocumentsStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.IStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.ProductStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.TransactionStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.VendorStaticListContainer
import ru.pavlig43.itemlist.statik.internal.di.moduleFactory

class StaticItemListFactoryComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    itemStaticListDependencies: ItemStaticListDependencies,
    itemListParamProvider: ItemListParamProvider
): ComponentContext by componentContext, SlotComponent {

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(moduleFactory(itemStaticListDependencies))
    private val _model = MutableStateFlow(SlotComponent.TabModel(itemListParamProvider.tabTitle))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    internal val listComponent: IStaticListContainer<out GenericItem, out IItemUi> = when(itemListParamProvider){
        is DocumentListParamProvider -> DocumentsStaticListContainer(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            paramProvider = itemListParamProvider,
            documentListRepository = scope.get()
        )

        is DeclarationListParamProvider -> DeclarationStaticListContainer(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            declarationListRepository = scope.get(),
            paramProvider = itemListParamProvider
        )

        is ProductListParamProvider -> ProductStaticListContainer(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            paramProvider = itemListParamProvider,
            productListRepository = scope.get()
        )

        is VendorListParamProvider -> VendorStaticListContainer(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            vendorListRepository = scope.get(),
            paramProvider = itemListParamProvider
        )

        is TransactionListParamProvider -> TransactionStaticListContainer(
            componentContext = componentContext,
            onCreate = onCreate,
            onItemClick = onItemClick,
            paramProvider = itemListParamProvider,
            listRepository = scope.get()
        )
    }

}


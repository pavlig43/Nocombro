package ru.pavlig43.productlist.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.productlist.internal.di.createModule
import ru.pavlig43.itemlist.api.component.IItemListComponent
import ru.pavlig43.itemlist.api.component.ItemListComponent
import ru.pavlig43.productlist.api.IProductLisDependencies

class ProductListComponent(
    componentContext: ComponentContext,
    onItemClick:(Int)-> Unit,
    onCreateScreen: () -> Unit,
    dependencies: IProductLisDependencies
) : ComponentContext by componentContext, IProductListComponent, SlotComponent {
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createModule(dependencies))


    private val _model = MutableStateFlow(SlotComponent.TabModel(TAB_TITLE))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    private companion object {
        const val TAB_TITLE = "Документы"
    }
    override val itemListComponent: IItemListComponent =
        ItemListComponent<Product,  ProductType>(
            componentContext = componentContext,
            fullListSelection = ProductType.entries,
            repository = scope.get(),
            onCreateScreen = onCreateScreen,
            onItemClick = onItemClick
        )

}

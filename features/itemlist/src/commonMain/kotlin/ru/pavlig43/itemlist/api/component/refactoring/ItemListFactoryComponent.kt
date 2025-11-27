package ru.pavlig43.itemlist.api.component.refactoring

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.itemlist.api.component.*
import ru.pavlig43.itemlist.internal.component.*
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
class MBSItemListComponent1(
    componentContext: ComponentContext,
    itemListParamProvider: ItemListParamProvider,
    onItemClick: (IItemUi) -> Unit,
    onCreate: () -> Unit,
    itemListDependencies: ItemListDependencies,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val itemListFactoryComponent = ItemListFactoryComponent(
        componentContext = childContext("itemlist"),
        onCreate = onCreate,
        onItemClick = onItemClick,
        itemListDependencies = itemListDependencies,
        itemListParamProvider = itemListParamProvider
    )

    fun onDismissClicked() {
        onDismissed()
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun  MBSItemList(component: MBSItemListComponent1, modifier: Modifier = Modifier){
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = component::onDismissClicked,
        sheetState = sheetState,
        modifier = modifier.fillMaxSize()){
        GeneralItemListScreen(component.itemListFactoryComponent)
    }
}
package ru.pavlig43.itemlist.statik.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.statik.api.ItemListParamProvider

@Suppress("UNCHECKED_CAST")
class MBSItemListComponent<I: IItemUi>(
    componentContext: ComponentContext,
    itemListParamProvider: ItemListParamProvider,
    onItemClick: (I) -> Unit,
    onCreate: () -> Unit,
    itemStaticListDependencies: ItemStaticListDependencies,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val staticItemListFactoryComponent = StaticItemListFactoryComponent(
        componentContext = childContext("itemlist"),
        onCreate = onCreate,
        onItemClick = { ui: IItemUi -> onItemClick(ui as I) },
        itemStaticListDependencies = itemStaticListDependencies,
        itemListParamProvider = itemListParamProvider
    )

    fun onDismissClicked() {
        onDismissed()
    }
}
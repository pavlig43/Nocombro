package ru.pavlig43.itemlist.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.itemlist.api.ItemListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi

@Suppress("UNCHECKED_CAST")
class MBSItemListComponent<I: IItemUi>(
    componentContext: ComponentContext,
    itemListParamProvider: ItemListParamProvider,
    onItemClick: (I) -> Unit,
    onCreate: () -> Unit,
    itemListDependencies: ItemListDependencies,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val itemListFactoryComponent = ItemListFactoryComponent(
        componentContext = childContext("itemlist"),
        onCreate = onCreate,
        onItemClick = { ui: IItemUi -> onItemClick(ui as I) },
        itemListDependencies = itemListDependencies,
        itemListParamProvider = itemListParamProvider
    )

    fun onDismissClicked() {
        onDismissed()
    }
}
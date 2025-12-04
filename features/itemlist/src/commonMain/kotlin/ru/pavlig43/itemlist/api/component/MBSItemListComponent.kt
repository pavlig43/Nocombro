package ru.pavlig43.itemlist.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.itemlist.api.ItemListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi

class MBSItemListComponent(
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
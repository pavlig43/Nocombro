package ru.pavlig43.itemlist.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.itemlist.api.data.ItemListRepository

class MBSItemListComponent<I: Item,S: ItemType>(
    private val componentContext: ComponentContext,
    fullListSelection: List<S>,
    onItemClick: (Int,String) -> Unit,
    repository: ItemListRepository<I, S>,
    onCreate: () -> Unit,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val itemList = ItemListComponent<I,S>(
        componentContext = childContext("itemList"),
        fullListSelection = fullListSelection,
        tabTitle = "",
        onCreate = onCreate,
        repository = repository,
        onItemClick = onItemClick,
        withCheckbox = false
    )

    fun onDismissClicked() {
        onDismissed()
    }

}
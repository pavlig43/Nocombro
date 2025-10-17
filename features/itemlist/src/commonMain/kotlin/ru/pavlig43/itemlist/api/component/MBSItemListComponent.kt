package ru.pavlig43.itemlist.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.itemlist.api.data.DefaultItemFilter
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemFilter

class MBSItemListComponent<I: Item,S: ItemType>(
    private val componentContext: ComponentContext,
    fullListSelection: List<S>,
    onItemClick: (IItemUi) -> Unit,
    repository: IItemListRepository<I, S>,
    onCreate: () -> Unit,
    filterFactory: (types: List<S>, searchText: String) -> ItemFilter<S> = { types, text -> DefaultItemFilter<S>(types,text) },
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val itemList = ItemListComponent<I,S>(
        componentContext = childContext("itemList"),
        fullListSelection = fullListSelection,
        tabTitle = "",
        onCreate = onCreate,
        repository = repository,
        onItemClick = onItemClick,
        filterFactory = filterFactory,
        withCheckbox = false
    )

    fun onDismissClicked() {
        onDismissed()
    }

}
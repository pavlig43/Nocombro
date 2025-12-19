package ru.pavlig43.itemlist.statik.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.core.refac.api.ImmutableTableBuilder2

@Suppress("UNCHECKED_CAST")
class MBSItemListComponent<I: IItemUi>(
    componentContext: ComponentContext,
    immutableTableBuilder: ImmutableTableBuilder2,
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
        immutableTableBuilder = immutableTableBuilder
    )

    fun onDismissClicked() {
        onDismissed()
    }
}
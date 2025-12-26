package ru.pavlig43.itemlist.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.itemlist.api.dependencies
import ru.pavlig43.itemlist.api.model.ITableUi

@Suppress("UNCHECKED_CAST")
class MBSImmutableTableComponent<I: ITableUi>(
    componentContext: ComponentContext,
    builderData: BuilderData<I>,
    onItemClick: (I) -> Unit,
    onCreate: () -> Unit,
    dependencies: dependencies,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val immutableTableComponentFactory = ImmutableTableComponentFactory(
        componentContext = childContext ("item_list"),
        dependencies = dependencies,
        builderData = builderData,
        onCreate = onCreate,
        onItemClick = { ui: ITableUi -> onItemClick(ui as I) },
    )


    fun onDismissClicked() {
        onDismissed()
    }
}
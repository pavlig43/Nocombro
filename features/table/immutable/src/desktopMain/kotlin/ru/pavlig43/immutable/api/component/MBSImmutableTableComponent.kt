package ru.pavlig43.immutable.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.tablecore.model.IMultiLineTableUi


@Suppress("UNCHECKED_CAST")
class MBSImmutableTableComponent<I: IMultiLineTableUi>(
    componentContext: ComponentContext,
    immutableTableBuilderData: ImmutableTableBuilderData<I>,
    onItemClick: (I) -> Unit,
    onCreate: () -> Unit,
    dependencies: ImmutableTableDependencies,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val immutableTableComponentFactory = ImmutableTableComponentFactoryMain(
        componentContext = childContext("item_list"),
        dependencies = dependencies,
        immutableTableBuilderData = immutableTableBuilderData,
        onCreate = onCreate,
        onItemClick = { ui: IMultiLineTableUi -> onItemClick(ui as I) },
    )


    fun onDismissClicked() {
        onDismissed()
    }
}
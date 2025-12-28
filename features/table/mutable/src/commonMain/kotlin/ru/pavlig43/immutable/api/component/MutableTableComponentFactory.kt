package ru.pavlig43.immutable.api.component

//import ru.pavlig43.itemlist.internal.component.ImmutableTableComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.immutable.api.MutableTableDependencies
import ru.pavlig43.immutable.internal.component.MutableTableComponent
import ru.pavlig43.immutable.internal.di.moduleFactory
import ru.pavlig43.tablecore.model.ITableUi

class MutableTableComponentFactory(
    componentContext: ComponentContext,
    dependencies: MutableTableDependencies,
    private val mutableTableBuilderData: MutableTableBuilderData<out ITableUi>,
) : ComponentContext by componentContext  {
    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(
        moduleFactory(
            dependencies
        )
    )

    internal val tableComponent = build<ITableUi>(context = childContext("table"))

    @Suppress("UNCHECKED_CAST")
    private fun <I : ITableUi> build(
        context: ComponentContext,
    ): MutableTableComponent<*, I, *, *> {
        return when (mutableTableBuilderData){
            CompositionTableBuilder -> TODO()
        }
    }
}
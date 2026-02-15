package ru.pavlig43.mutable.api.flowMiltiline.component

import androidx.compose.runtime.Immutable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.immutable.api.component.ImmutableTableComponentFactoryMain

abstract class FlowMultilineComponent(
    componentContext: ComponentContext
):ComponentContext by componentContext, FormTabComponent {

    private val observableIds = MutableStateFlow(emptyList<Int>())


    val immutableTableComponent: ImmutableTableComponentFactoryMain = ImmutableTableComponentFactoryMain(
        componentContext = childContext("immutableTable"),
        dependencies = TODO(),
        immutableTableBuilderData = TODO(),
        onCreate = TODO(),
        onItemClick = TODO(),
    )

}
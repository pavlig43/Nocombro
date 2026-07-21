package ru.pavlig43.sampletable.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.sampletable.viewmodel.SampleViewModel

interface SampleTableComponent : MainTabComponent

class SampleTableComponentMain(
    componentContext: ComponentContext,
) : SampleTableComponent, ComponentContext by componentContext {
    internal val viewModel = SampleViewModel(componentContext)

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Пример таблицы"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()
}

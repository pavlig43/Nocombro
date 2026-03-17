package ru.pavlig43.money.main.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.MainTabComponent

class MainComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext, MainTabComponent {

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Деньги"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    fun onProfitabilityClick() {
        // Пустой колбэк для будущего использования
    }
}

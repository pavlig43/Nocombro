package ru.pavlig43.main.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.tabs.TabOpener

class AnalyticMainComponent(
    componentContext: ComponentContext,
    private val tabOpener: TabOpener,
) : ComponentContext by componentContext, MainTabComponent {

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Деньги"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    internal fun onOpenTab(item: ItemNavigation) {
        when (item) {
            ItemNavigation.PROFITABILITY -> {tabOpener.openProfitabilityTab()}
        }
    }

}
internal enum class ItemNavigation(val title: String){
    PROFITABILITY("Прибыльность")

}

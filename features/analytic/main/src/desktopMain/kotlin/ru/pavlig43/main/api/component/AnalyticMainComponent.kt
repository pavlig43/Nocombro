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

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Аналитика"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    internal fun onOpenTab(item: ItemNavigation) {
        when (item) {
            ItemNavigation.PROFITABILITY -> {tabOpener.openProfitabilityTab()}
        }
    }

}
enum class ItemNavigation(
    val title: String,
    val subtitle: String,
    val description: String,
){
    PROFITABILITY(
        title = "Прибыльность",
        subtitle = "Сводка по товарам, партиям и финансовому результату.",
        description = "Открывает таблицу прибыльности с периодом, фильтрами, сортировкой и деталями по партиям.",
    )

}

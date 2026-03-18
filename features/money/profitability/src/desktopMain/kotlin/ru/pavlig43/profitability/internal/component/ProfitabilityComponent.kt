package ru.pavlig43.profitability.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.datetime.period.dateTime.DateTimePeriodComponent
import ru.pavlig43.profitability.api.ProfitabilityDependencies
import ru.pavlig43.profitability.internal.di.createModule

class ProfitabilityComponent(
    componentContext: ComponentContext,
    dependencies: ProfitabilityDependencies
):ComponentContext by componentContext, MainTabComponent {

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Прибыльность"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    val dateTimePeriodComponent = DateTimePeriodComponent(
        componentContext = childContext("date_time"),
        initDTPeriod = DTPeriod.thisMonth
    )
    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(createModule(dependencies))


}
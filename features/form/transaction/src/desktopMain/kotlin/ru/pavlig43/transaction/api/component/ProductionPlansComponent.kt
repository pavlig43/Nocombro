package ru.pavlig43.transaction.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.transaction.internal.production.ProductionPlanRepository

class ProductionPlansComponent(
    componentContext: ComponentContext,
    db: NocombroDatabase,
    internal val openPlan: (Int) -> Unit,
) : ComponentContext by componentContext, MainTabComponent {
    override val model = MutableStateFlow(MainTabComponent.NavTabState("Планы производства"))
    internal val plans = ProductionPlanRepository(db).plans
}

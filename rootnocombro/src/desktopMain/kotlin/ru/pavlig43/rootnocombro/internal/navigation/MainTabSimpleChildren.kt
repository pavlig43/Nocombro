package ru.pavlig43.rootnocombro.internal.navigation

import com.arkivanov.decompose.ComponentContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.doctor.api.component.DoctorComponent
import ru.pavlig43.main.api.component.AnalyticMainComponent
import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.profitability.internal.component.ProfitabilityComponent
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.BatchMovementChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.DoctorChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.MainMoneyChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.NotificationChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ProfitabilityChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.SampleTableChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.StorageChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.AnalyticConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.BatchMovementListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.DoctorConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.NotificationConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ProfitabilityConfig
import ru.pavlig43.sampletable.api.component.SampleTableComponentMain
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementComponent
import ru.pavlig43.storage.api.component.storage.StorageComponent

internal fun createSimpleMainTabChild(
    config: MainTabConfig,
    context: ComponentContext,
    scope: Scope,
    tabOpener: TabOpener,
    notificationComponent: NotificationComponent,
): MainTabChild? =
    when (config) {
        is NotificationConfig -> NotificationChild(notificationComponent)

        is AnalyticConfig -> MainMoneyChild(
            AnalyticMainComponent(
                componentContext = context,
                tabOpener = tabOpener,
            )
        )

        is MainTabConfig.SampleTableConfig -> SampleTableChild(
            SampleTableComponentMain(
                componentContext = context,
            )
        )

        is MainTabConfig.StorageConfig -> StorageChild(
            StorageComponent(
                componentContext = context,
                dependencies = scope.get(),
                tabOpener = tabOpener,
            )
        )

        is ProfitabilityConfig -> ProfitabilityChild(
            ProfitabilityComponent(
                componentContext = context,
                dependencies = scope.get(),
            )
        )

        is DoctorConfig -> DoctorChild(
            DoctorComponent(
                componentContext = context,
                dependencies = scope.get(),
            )
        )

        is BatchMovementListConfig -> BatchMovementChild(
            BatchMovementComponent(
                componentContext = context,
                dependencies = scope.get(),
                tabOpener = tabOpener,
                batchId = config.batchId,
                productName = config.productName,
                initStart = config.start,
                initEnd = config.end,
            )
        )

        else -> null
    }

package ru.pavlig43.nocombro.mobile.api.component

import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.sync.MobileSyncRepository
import ru.pavlig43.nocombro.mobile.sync.MobileSyncReportStore
import ru.pavlig43.nocombro.mobile.sync.MobileSyncStateStore

/**
 * Зависимости, которые корневой компонент передаёт экранам Android.
 */
class NocombroMobileRootDependencies(
    val experimentsDependencies: ExperimentDependencies,
    val syncRepository: MobileSyncRepository,
    val syncStateStore: MobileSyncStateStore,
    val syncReportStore: MobileSyncReportStore,
    val fileProviderAuthority: String,
)

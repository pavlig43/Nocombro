package ru.pavlig43.nocombro.mobile.api.component

import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.sync.MobileSyncRepository

/**
 * Зависимости, которые root component отдаёт Android-экранам.
 */
class NocombroMobileRootDependencies(
    val experimentsDependencies: ExperimentDependencies,
    val syncRepository: MobileSyncRepository,
)

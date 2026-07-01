package ru.pavlig43.nocombro.mobile.internal.navigation

import kotlinx.serialization.Serializable
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentsMobileComponent

@Serializable
sealed interface MobileConfig {
    @Serializable
    data object Menu : MobileConfig

    @Serializable
    data object Experiments : MobileConfig
}

sealed interface MobileChild {
    data object Menu : MobileChild

    class Experiments(
        val component: ExperimentsMobileComponent,
    ) : MobileChild
}

package ru.pavlig43.nocombro.mobile.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentsMobileComponent
import ru.pavlig43.nocombro.mobile.internal.navigation.MobileChild
import ru.pavlig43.nocombro.mobile.internal.navigation.MobileConfig

class NocombroMobileRootComponent(
    componentContext: ComponentContext,
    private val dependencies: NocombroMobileRootDependencies,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<MobileConfig>()

    val menuItems: List<MobileMenuItem> = listOf(
        MobileMenuItem(
            config = MobileConfig.Experiments,
            title = "Эксперименты",
        )
    )

    val stack: Value<ChildStack<MobileConfig, MobileChild>> = childStack(
        source = navigation,
        serializer = MobileConfig.serializer(),
        initialConfiguration = MobileConfig.Menu,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    fun selectMenuItem(config: MobileConfig) {
        navigation.pushToFront(config)
    }

    fun openMenu() {
        navigation.pushToFront(MobileConfig.Menu)
    }

    private fun createChild(
        config: MobileConfig,
        componentContext: ComponentContext,
    ): MobileChild {
        return when (config) {
            MobileConfig.Menu -> MobileChild.Menu
            MobileConfig.Experiments -> MobileChild.Experiments(
                ExperimentsMobileComponent(
                    componentContext = componentContext,
                    dependencies = dependencies.experimentsDependencies,
                )
            )
        }
    }
}

class NocombroMobileRootDependencies(
    val experimentsDependencies: ExperimentDependencies,
)

data class MobileMenuItem(
    val config: MobileConfig,
    val title: String,
)

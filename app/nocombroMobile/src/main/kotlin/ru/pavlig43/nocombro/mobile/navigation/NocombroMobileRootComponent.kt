package ru.pavlig43.nocombro.mobile.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.pavlig43.nocombro.mobile.experiments.ExperimentsMobileComponent
import ru.pavlig43.nocombro.mobile.experiments.ExperimentsMobileDependencies

/**
 * Root Decompose-компонент mobile-приложения.
 *
 * Держит меню, стек экранов и создаёт child-компоненты фич.
 */
class NocombroMobileRootComponent(
    componentContext: ComponentContext,
    private val dependencies: NocombroMobileRootDependencies,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<MobileConfig>()

    /**
     * Пункты главного mobile-меню.
     */
    val menuItems: List<MobileMenuItem> = listOf(
        MobileMenuItem(
            config = MobileConfig.Experiments,
            title = "Эксперименты",
        )
    )

    /**
     * Активный стек экранов mobile-приложения.
     */
    val stack: Value<ChildStack<MobileConfig, MobileChild>> = childStack(
        source = navigation,
        serializer = MobileConfig.serializer(),
        initialConfiguration = MobileConfig.Menu,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    /**
     * Открывает экран из главного меню.
     */
    fun selectMenuItem(config: MobileConfig) {
        navigation.pushToFront(config)
    }

    /**
     * Возвращает к главному меню.
     */
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

/**
 * Зависимости root-компонента mobile-приложения.
 */
class NocombroMobileRootDependencies(
    val experimentsDependencies: ExperimentsMobileDependencies,
)

/**
 * Описание пункта главного mobile-меню.
 */
data class MobileMenuItem(
    val config: MobileConfig,
    val title: String,
)

/**
 * Serializable config экранов mobile-приложения.
 */
@Serializable
sealed interface MobileConfig {
    /**
     * Главное mobile-меню.
     */
    @Serializable
    data object Menu : MobileConfig

    /**
     * Экран журнала экспериментов.
     */
    @Serializable
    data object Experiments : MobileConfig
}

/**
 * Child-экран, созданный из [MobileConfig].
 */
sealed interface MobileChild {
    /**
     * Child главного mobile-меню.
     */
    data object Menu : MobileChild

    /**
     * Child с компонентом журнала экспериментов.
     */
    class Experiments(
        val component: ExperimentsMobileComponent,
    ) : MobileChild
}

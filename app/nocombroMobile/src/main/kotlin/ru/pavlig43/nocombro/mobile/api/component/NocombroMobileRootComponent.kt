package ru.pavlig43.nocombro.mobile.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentsMobileComponent
import ru.pavlig43.nocombro.mobile.sync.MobileSyncComponent

/**
 * Корневой компонент Android-сборки: управляет меню, панелью синхронизации и стековой навигацией.
 */
class NocombroMobileRootComponent(
    componentContext: ComponentContext,
    private val dependencies: NocombroMobileRootDependencies,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<MobileConfig>()

    val menuItems: List<MobileMenuItem> = listOf(
        MobileMenuItem(
            config = MobileConfig.Experiments,
            title = "Эксперименты",
        ),
    )

    /** Общий компонент панели синхронизации. */
    val syncComponent = MobileSyncComponent(
        componentContext = componentContext,
        repository = dependencies.syncRepository,
        stateStore = dependencies.syncStateStore,
        reportStore = dependencies.syncReportStore,
        fileProviderAuthority = dependencies.fileProviderAuthority,
    )

    val stack: Value<ChildStack<MobileConfig, MobileChild>> = childStack(
        source = navigation,
        serializer = MobileConfig.serializer(),
        initialConfiguration = MobileConfig.Menu,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    /**
     * Открывает выбранный пункт главного меню.
     */
    fun selectMenuItem(config: MobileConfig) {
        navigation.pushToFront(config)
    }

    /**
     * Возвращает корневой стек к главному меню.
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
 * Пункт главного меню Android-приложения.
 */
data class MobileMenuItem(
    val config: MobileConfig,
    val title: String,
)

/**
 * Сериализуемые маршруты корневой стековой навигации.
 */
@Serializable
sealed interface MobileConfig {
    @Serializable
    data object Menu : MobileConfig

    @Serializable
    data object Experiments : MobileConfig

}

/**
 * Дочерние экраны, которые создаются из [MobileConfig].
 */
sealed interface MobileChild {
    data object Menu : MobileChild

    class Experiments(
        val component: ExperimentsMobileComponent,
    ) : MobileChild
}

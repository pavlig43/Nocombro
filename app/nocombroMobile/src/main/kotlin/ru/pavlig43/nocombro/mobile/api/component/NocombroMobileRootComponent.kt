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
 * Root component Android-сборки: держит меню, sync-панель и stack-навигацию.
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
        )
    )

    /**
     * Общий sync component меню и экрана просмотра расхождений.
     */
    val syncComponent = MobileSyncComponent(
        componentContext = componentContext,
        repository = dependencies.syncRepository,
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
     * Возвращает root stack к главному меню.
     */
    fun openMenu() {
        navigation.pushToFront(MobileConfig.Menu)
    }

    /**
     * Открывает экран preview для локальных и remote sync-правок.
     */
    fun openSyncChanges() {
        navigation.pushToFront(MobileConfig.SyncChanges)
    }

    private fun createChild(
        config: MobileConfig,
        componentContext: ComponentContext,
    ): MobileChild {
        return when (config) {
            MobileConfig.Menu -> MobileChild.Menu
            MobileConfig.SyncChanges -> MobileChild.SyncChanges(syncComponent)
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
 * Serializable routes root stack-навигации.
 */
@Serializable
sealed interface MobileConfig {
    @Serializable
    data object Menu : MobileConfig

    @Serializable
    data object Experiments : MobileConfig

    @Serializable
    data object SyncChanges : MobileConfig
}

/**
 * Экранные children, которые создаются из [MobileConfig].
 */
sealed interface MobileChild {
    data object Menu : MobileChild

    class SyncChanges(
        val component: MobileSyncComponent,
    ) : MobileChild

    class Experiments(
        val component: ExperimentsMobileComponent,
    ) : MobileChild
}

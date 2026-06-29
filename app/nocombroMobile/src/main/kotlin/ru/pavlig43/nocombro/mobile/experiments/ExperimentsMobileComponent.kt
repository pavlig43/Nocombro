package ru.pavlig43.nocombro.mobile.experiments

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase

/**
 * Зависимости mobile-компонента экспериментов.
 */
class ExperimentDependencies(
    val database: MobileExperimentsDatabase,
)

/**
 * Decompose-компонент mobile-экрана экспериментов.
 */
class ExperimentsMobileComponent(
    componentContext: ComponentContext,
    private val dependencies: ExperimentDependencies,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<ExperimentsMobileConfig>()

    /**
     * Стек экранов внутри mobile-фичи экспериментов.
     */
    val stack: Value<ChildStack<ExperimentsMobileConfig, ExperimentsMobileChild>> = childStack(
        source = navigation,
        serializer = ExperimentsMobileConfig.serializer(),
        initialConfiguration = ExperimentsMobileConfig.List,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    /**
     * Выбирает эксперимент по локальному id.
     */
    fun selectExperiment(id: Int) {
        navigation.pushToFront(ExperimentsMobileConfig.Details(id))
    }

    /**
     * Возвращает из эксперимента к списку.
     */
    fun closeExperimentDetails() {
        navigation.pop()
    }

    private fun createChild(
        config: ExperimentsMobileConfig,
        componentContext: ComponentContext,
    ): ExperimentsMobileChild {
        return when (config) {
            ExperimentsMobileConfig.List -> ExperimentsMobileChild.List(
                ExperimentsListComponent(
                    componentContext = componentContext,
                    dependencies = dependencies,
                )
            )
            is ExperimentsMobileConfig.Details -> {
                ExperimentsMobileChild.Details(config.experimentId)
            }
        }
    }
}

/**
 * Serializable config вложенной навигации experiments-фичи.
 */
@Serializable
sealed interface ExperimentsMobileConfig {
    /**
     * Список экспериментов.
     */
    @Serializable
    data object List : ExperimentsMobileConfig

    /**
     * Экран одного эксперимента.
     */
    @Serializable
    data class Details(
        val experimentId: Int,
    ) : ExperimentsMobileConfig
}

/**
 * Child-экран experiments-фичи.
 */
sealed interface ExperimentsMobileChild {
    /**
     * Child списка экспериментов.
     */
    class List(
        val component: ExperimentsListComponent,
    ) : ExperimentsMobileChild

    /**
     * Child экрана одного эксперимента.
     */
    data class Details(
        val experimentId: Int,
    ) : ExperimentsMobileChild
}

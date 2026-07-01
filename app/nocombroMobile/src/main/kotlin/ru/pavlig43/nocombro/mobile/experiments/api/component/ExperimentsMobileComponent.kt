package ru.pavlig43.nocombro.mobile.experiments.api.component
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentDetailsComponent
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentEntryComponent
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentsListComponent

class ExperimentsMobileComponent(
    componentContext: ComponentContext,
    private val dependencies: ExperimentDependencies,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<ExperimentsMobileConfig>()

    val stack: Value<ChildStack<ExperimentsMobileConfig, ExperimentsMobileChild>> = childStack(
        source = navigation,
        serializer = ExperimentsMobileConfig.serializer(),
        initialConfiguration = ExperimentsMobileConfig.List,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    fun selectExperiment(id: Int) {
        navigation.pushToFront(ExperimentsMobileConfig.Details(id))
    }

    fun openEntry(entryId: Int) {
        navigation.pushToFront(ExperimentsMobileConfig.Entry(entryId))
    }

    fun closeCurrentScreen() {
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
                    onExperimentCreated = ::selectExperiment,
                )
            )
            is ExperimentsMobileConfig.Details -> {
                ExperimentsMobileChild.Details(
                    ExperimentDetailsComponent(
                        componentContext = componentContext,
                        experimentId = config.experimentId,
                        dependencies = dependencies,
                        onEntryOpened = ::openEntry,
                    )
                )
            }
            is ExperimentsMobileConfig.Entry -> {
                ExperimentsMobileChild.Entry(
                    ExperimentEntryComponent(
                        componentContext = componentContext,
                        entryId = config.entryId,
                        dependencies = dependencies,
                    )
                )
            }
        }
    }
}

@Serializable
sealed interface ExperimentsMobileConfig {
    @Serializable
    data object List : ExperimentsMobileConfig

    @Serializable
    data class Details(
        val experimentId: Int,
    ) : ExperimentsMobileConfig

    @Serializable
    data class Entry(
        val entryId: Int,
    ) : ExperimentsMobileConfig
}

sealed interface ExperimentsMobileChild {
    class List(
        val component: ExperimentsListComponent,
    ) : ExperimentsMobileChild

    class Details(
        val component: ExperimentDetailsComponent,
    ) : ExperimentsMobileChild

    class Entry(
        val component: ExperimentEntryComponent,
    ) : ExperimentsMobileChild
}

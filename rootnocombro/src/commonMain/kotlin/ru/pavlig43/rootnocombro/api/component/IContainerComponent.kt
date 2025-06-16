package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.children.ChildNavState
import com.arkivanov.decompose.router.children.NavState
import com.arkivanov.decompose.router.children.SimpleChildNavState
import com.arkivanov.decompose.router.children.SimpleNavigation
import com.arkivanov.decompose.router.children.children
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import ru.pavlig43.rootnocombro.api.component.TabNavigationComponent.Children



interface TabNavigationComponent<out TabComponent : Any> {
    val children: Value<Children<*, TabComponent>>

    fun onSelect(index: Int)
    fun onMove(fromIndex: Int, toIndex: Int)
    fun onTabCloseClicked(index: Int)

    class Children<out C : Any, out T : Any>(
        val items: List<Child.Created<C, T>>,
        val selected: Int?,
    )
}

class DefaultTabNavigationComponent<Configuration : Any, TabComponent : Any>(
    componentContext: ComponentContext,
    private val configurations: List<Configuration>,
    serializer: KSerializer<Configuration>,
    private val tabFactory: (componentContext: ComponentContext, config: Configuration) -> TabComponent
) : ComponentContext by componentContext, TabNavigationComponent<TabComponent> {
    private val navigation = SimpleNavigation<(NavigationState<Configuration>) -> NavigationState<Configuration>>()

    override val children: Value<Children<Configuration, TabComponent>> = children(
        source = navigation,
        stateSerializer = NavigationState.serializer(serializer),
        key = "tabs",
        initialState = {
            NavigationState(configurations = configurations, index = 0)
        },
        navTransformer = { state, transformer -> transformer(state) },
        stateMapper = { state, children ->
            Children(
                items = children.map { it as Child.Created },
                selected = state.index,
            )
        },
        backTransformer = { state ->
            state.takeIf { it.index != null }
                ?.takeIf { it.index!! > 0 }
                ?.let { eligibleState ->
                    { eligibleState.copy(index = eligibleState.index!! - 1) }
                }
        },
        childFactory = { configuration, componentContext ->
            tabFactory(componentContext, configuration)
        },
    )

    override fun onSelect(index: Int) {
        require(index in 0..configurations.size)
        navigation.navigate { state ->
            state.copy(index = index)
        }
    }

    override fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in configurations.indices || toIndex !in configurations.indices) return

        navigation.navigate { state ->
            val updatedConfigurations = state.configurations.toMutableList()
            val movedItem = updatedConfigurations.removeAt(fromIndex)
            updatedConfigurations.add(toIndex, movedItem)

            state.copy(configurations = updatedConfigurations, index = toIndex)
        }
    }

    override fun onTabCloseClicked(index: Int) {
        if (index !in configurations.indices) return

        navigation.navigate { state ->
            val updatedConfigurations = state.configurations.toMutableList()
            updatedConfigurations.removeAt(index)

            val newIndex = if (updatedConfigurations.isEmpty()) {
                null
            } else {
                index.coerceAtLeast(0).coerceAtMost(updatedConfigurations.size - 1)
            }

            state.copy(configurations = updatedConfigurations, index = newIndex)
        }
    }

    @Serializable
    private data class NavigationState<Configuration : Any>(
        val configurations: List<Configuration>,
        val index: Int?,
    ) : NavState<Configuration> {

        override val children: List<SimpleChildNavState<Configuration>> by lazy {
            configurations.mapIndexed { index, config ->
                val status = if (index == this.index) ChildNavState.Status.RESUMED else ChildNavState.Status.CREATED
                SimpleChildNavState(
                    configuration = config,
                    status = status,
                )
            }
        }
    }
}


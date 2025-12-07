package ru.pavlig43.core.tabs

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


class TabNavigationComponent<TabConfiguration : Any, SlotComponent : Any>(
    componentContext: ComponentContext,
    private val startConfigurations: List<TabConfiguration>,
    serializer: KSerializer<TabConfiguration>,
    private val slotFactory: (
        componentContext: ComponentContext,
        config: TabConfiguration,
        openNewTab: (TabConfiguration) -> Unit,
        closeTab: () -> Unit,
    ) -> SlotComponent
) : ComponentContext by componentContext {
    class Children<out C : Any, out T : Any>(
        val items: List<Child.Created<C, T>>,
        val selectedIndex: Int?,
    )
    private val navigation =
        SimpleNavigation<(NavigationState<TabConfiguration>) -> NavigationState<TabConfiguration>>()

    val children: Value<Children<TabConfiguration, SlotComponent>> =
        children<
                ComponentContext,
                TabConfiguration,
                SlotComponent,
                    (NavigationState<TabConfiguration>) -> NavigationState<TabConfiguration>,
                NavigationState<TabConfiguration>,
                Children<TabConfiguration, SlotComponent>
                >(
            source = navigation,
            stateSerializer = NavigationState.serializer(serializer),
            key = "tabs",
            initialState = {
                NavigationState(configurations = startConfigurations, currentIndex = 0)
            },
            navTransformer = { state, transformer: (NavigationState<TabConfiguration>) ->
            NavigationState<TabConfiguration> ->
                transformer(state)
            },
            stateMapper = { state, children ->
                Children(
                    items = children.map { it as Child.Created },
                    selectedIndex = state.currentIndex,
                )
            },
            backTransformer = { state ->
                state.takeIf { it.currentIndex != null }
                    ?.takeIf { it.currentIndex!! > 0 }
                    ?.let { eligibleState ->
                        { eligibleState.copy(currentIndex = eligibleState.currentIndex!! - 1) }
                    }
            },
            childFactory = { configuration, componentContext ->
                slotFactory(
                    componentContext,
                    configuration,
                    ::addTab,
                    { onCloseTab(configuration) }
                )
            },
        )

    fun onSelectTab(index: Int) {
        navigation.navigate { state ->
            require(index in 0..state.configurations.size)
            state.copy(currentIndex = index)
        }
    }

    fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in children.value.items.indices || toIndex !in children.value.items.indices) return

        navigation.navigate { state ->
            val updatedConfigurations = state.configurations.toMutableList()
            val movedItem = updatedConfigurations.removeAt(fromIndex)
            updatedConfigurations.add(toIndex, movedItem)

            state.copy(configurations = updatedConfigurations, currentIndex = toIndex)
        }
    }

    private fun onCloseTab(tabConfiguration: TabConfiguration) {
        val index = children.value.items.indexOfFirst { it.configuration === tabConfiguration }
        onTabCloseClicked(index)
    }

    fun onTabCloseClicked(index: Int) {

        navigation.navigate { state ->
            if (index !in state.configurations.indices) return@navigate state
            val updatedConfigurations = state.configurations.toMutableList()
            updatedConfigurations.removeAt(index)


            val newIndex = when {
                updatedConfigurations.isEmpty() -> null
                state.currentIndex == null -> null
                index == state.currentIndex -> index.coerceIn(0, updatedConfigurations.size - 1)
                index > state.currentIndex -> state.currentIndex
//                else -> state.currentIndex - 1
                else -> state.currentIndex - 1
            }


            state.copy(configurations = updatedConfigurations, currentIndex = newIndex)
        }
    }

    fun addTab(configuration: TabConfiguration) {
        navigation.navigate { state ->
            val updatedConfigurations = state.configurations.toMutableList()

            updatedConfigurations.add(configuration)
            val lastIndex = updatedConfigurations.lastIndex
            state.copy(configurations = updatedConfigurations, currentIndex = lastIndex)
        }
    }

    @Serializable
    private data class NavigationState<TabConfiguration : Any>(
        val configurations: List<TabConfiguration>,
        val currentIndex: Int?,
    ) : NavState<TabConfiguration> {

        override val children: List<SimpleChildNavState<TabConfiguration>> by lazy {
            configurations.mapIndexed { index, config ->
//                val status =
//                    if (index == this.currentIndex) ChildNavState.Status.RESUMED else ChildNavState.Status.CREATED
                SimpleChildNavState(
                    configuration = config,
                    status = ChildNavState.Status.RESUMED,
                )

            }
        }
    }
}
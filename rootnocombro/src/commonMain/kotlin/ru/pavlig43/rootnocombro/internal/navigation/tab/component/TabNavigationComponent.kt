package ru.pavlig43.rootnocombro.internal.navigation.tab.component

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
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.TabNavigationComponent.Children

internal interface TabNavigationComponent<TabConfiguration : Any, SlotComponent : Any> {

    val children: Value<Children<*, SlotComponent>>
    fun onSelect(index: Int)
    fun onMove(fromIndex: Int, toIndex: Int)
    fun onTabCloseClicked(index: Int)
    fun addTab(configuration: TabConfiguration)

    class Children<out C : Any, out T : Any>(
        val items: List<Child.Created<C, T>>,
        val selected: Int?,
    )
}

internal class DefaultTabNavigationComponent<TabConfiguration : Any, SlotComponent : Any>(
    componentContext: ComponentContext,
    private val startConfigurations: List<TabConfiguration>,
    serializer: KSerializer<TabConfiguration>,
    private val slotFactory: (
        componentContext: ComponentContext,
        config: TabConfiguration,
        openNewTab: (TabConfiguration) -> Unit
    ) -> SlotComponent
) : ComponentContext by componentContext, TabNavigationComponent<TabConfiguration, SlotComponent> {
    private val navigation =
        SimpleNavigation<(NavigationState<TabConfiguration>) -> NavigationState<TabConfiguration>>()


    override val children: Value<Children<TabConfiguration, SlotComponent>> =
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
                NavigationState(configurations = startConfigurations, index = 0)
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
                slotFactory(componentContext, configuration, ::addTab)
            },
        )

    override fun onSelect(index: Int) {
        navigation.navigate { state ->
            require(index in 0..state.configurations.size)
            state.copy(index = index)
        }
    }

    override fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in startConfigurations.indices || toIndex !in startConfigurations.indices) return

        navigation.navigate { state ->
            val updatedConfigurations = state.configurations.toMutableList()
            val movedItem = updatedConfigurations.removeAt(fromIndex)
            updatedConfigurations.add(toIndex, movedItem)

            state.copy(configurations = updatedConfigurations, index = toIndex)
        }
    }

    override fun onTabCloseClicked(index: Int) {


        navigation.navigate { state ->
            if (index !in state.configurations.indices) return@navigate state
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

    override fun addTab(configuration: TabConfiguration) {
        navigation.navigate { state ->
            val updatedConfigurations = state.configurations.toMutableList()

            updatedConfigurations.add(configuration)
            val lastIndex = updatedConfigurations.lastIndex
            state.copy(configurations = updatedConfigurations, index = lastIndex)
        }
    }

    @Serializable
    private data class NavigationState<TabConfiguration : Any>(
        val configurations: List<TabConfiguration>,
        val index: Int?,
    ) : NavState<TabConfiguration> {

        override val children: List<SimpleChildNavState<TabConfiguration>> by lazy {
            configurations.mapIndexed { index, config ->
                val status =
                    if (index == this.index) ChildNavState.Status.RESUMED else ChildNavState.Status.CREATED
                SimpleChildNavState(
                    configuration = config,
                    status = status,
                )
            }
        }
    }
}

class Level // TODO empty log level

fun <P1, P2, R> Function2<P1, P2, R>.curried(): (P1) -> (P2) -> R =
    { p1: P1 -> { p2: P2 -> this(p1, p2) } }

fun <P1, P2, P3, R> Function3<P1, P2, P3, R>.curried(): (P1) -> (P2) -> (P3) -> R =
    { p1 -> { p2 -> { p3 -> this(p1, p2, p3) } } }

fun logger(level: Level, appender: Appendable, msg: String): Unit = println(msg)

fun curryExample() {
    val logger: (Level) -> (Appendable) -> (String) -> Unit = ::logger.curried()
    val logger2: (Appendable) -> (String) -> Unit = logger(Level())
    val logger3: (String) -> Unit = logger2(System.out)
    logger3("my message")
}

fun main() {
    curryExample()
}
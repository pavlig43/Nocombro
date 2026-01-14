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

/**
 * Компонент навигации по вкладкам с динамическим управлением.
 * Смотреть для примера реализованный в библиотеке пример https://github.com/arkivanov/Decompose/blob/f82b212f/decompose/src/commonMain/kotlin/com/arkivanov/decompose/router/stack/ChildStackFactory.kt#L171-L176
 *
 *
 * @param TabConfiguration Тип конфигурации вкладки
 * @param TabChild Тип Child, использовать как sealed,
 * в конструктор которого передавать компонент, отвечающий за содержимое вкладки как верхней так и самого экрана
 * @param startConfigurations Начальный список конфигураций вкладок
 * @param serializer Сериализатор для конфигураций (для сохранения состояния при смерти приложения )
 * @param tabChildFactory Фабрика для создания TabChild по конфигурации
 */

class TabNavigationComponent<TabConfiguration : Any, out TabChild : Any>(
    componentContext: ComponentContext,
    private val startConfigurations: List<TabConfiguration>,
    serializer: KSerializer<TabConfiguration>,
    private val tabChildFactory: (
        componentContext: ComponentContext,
        config: TabConfiguration,
        closeTab: () -> Unit,
    ) -> TabChild
) : ComponentContext by componentContext {

    /**
     * [items] список [Child.Created<C, T>] где С это конфигурация, а Т - [TabChild]
     * [selectedIndex] индекс выбранной вкладки
     */
    class TabChildren<out C : Any, out T : Any>(
        val items: List<Child.Created<C, T>>,
        val selectedIndex: Int?,
    )

    /**
     * Класс, поставляющий навигационные эвенты.
     * В [SimpleNavigation] дженерик тип - это тип эвента.
     * В нашем случае это лямбда, которая принимает приватный стэйт [TabConfigurationState],
     * обновляет его и возвращает измененный.
     */
    private val navigation =
        SimpleNavigation<(TabConfigurationState<TabConfiguration>) -> TabConfigurationState<TabConfiguration>>()

    /**
     *Аналог StateFlow<[TabChildren]>.
     */
    val tabChildren: Value<TabChildren<TabConfiguration, TabChild>> =
        children<
                ComponentContext,
                TabConfiguration,
                TabChild,
                    (TabConfigurationState<TabConfiguration>) -> TabConfigurationState<TabConfiguration>,
                TabConfigurationState<TabConfiguration>,
                TabChildren<TabConfiguration, TabChild>
                >(
            source = navigation,
            stateSerializer = TabConfigurationState.serializer(typeSerial0 = serializer),
            key = "tabs",
            initialState = {
                TabConfigurationState(configurations = startConfigurations, currentIndex = 0)
            },
            navTransformer = { state, event: (TabConfigurationState<TabConfiguration>) ->
            TabConfigurationState<TabConfiguration> ->
                event(state)
            },
            stateMapper = { state, children ->
                TabChildren(
                    items = children.map { child: Child<TabConfiguration, TabChild> -> child as Child.Created },
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
                tabChildFactory(
                    componentContext,
                    configuration,
                    { onCloseTab(configuration) }
                )
            },
        )

    fun onSelectTab(index: Int) {
        navigation.navigate { state: TabConfigurationState<TabConfiguration> ->
            require(index in 0..state.configurations.size)
            state.copy(currentIndex = index)
        }
    }

    fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in tabChildren.value.items.indices || toIndex !in tabChildren.value.items.indices) return

        navigation.navigate { state: TabConfigurationState<TabConfiguration> ->
            val updatedConfigurations = state.configurations.toMutableList()
            val movedItem = updatedConfigurations.removeAt(fromIndex)
            updatedConfigurations.add(toIndex, movedItem)

            state.copy(configurations = updatedConfigurations, currentIndex = toIndex)
        }
    }

    private fun onCloseTab(tabConfiguration: TabConfiguration) {
        val index = tabChildren.value.items.indexOfFirst { it.configuration === tabConfiguration }
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

    /**
     * Класс состояние для
     * [configurations] списка конфигураций(на главной например: список документов, или детали этого документа)
     * [currentIndex] индекс выбранной вкладки, может быть null, если список конфигураций пуст
     * Все вкладки независимо выбраны они или нет находятся в активном состоянии [ChildNavState.Status.RESUMED].
     * Реализует интерфейс [NavState], который содержит в себе просто список конфигураций, которые имеют свой статус.
     */
    @Serializable
    private data class TabConfigurationState<TabConfiguration : Any>(
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
package ru.pavlig43.notification.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.toStateFlow
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.notification.api.NotificationDependencies
import ru.pavlig43.notification.api.data.NotificationDrawerUi
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.component.ILevelNotificationComponent
import ru.pavlig43.notification.internal.component.LevelNotificationComponent
import ru.pavlig43.notification.internal.di.createNotificationModule


class NotificationComponent(
    componentContext: ComponentContext,
    private val onOpenTab: (NotificationItem, Int) -> Unit,
    dependencies: NotificationDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Оповещения"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(
        createNotificationModule(dependencies)
    )
    private val coroutineScope = componentCoroutineScope()


    internal val tabNavigationComponent: TabNavigationComponent<NotificationLevel, ILevelNotificationComponent> =
        TabNavigationComponent(
            componentContext = childContext("notification_tab"),
            startConfigurations = listOf(
                NotificationLevel.Zero,
                NotificationLevel.One,
                NotificationLevel.Two,
            ),
            serializer = NotificationLevel.serializer(),
            tabChildFactory = { context, tabConfig: NotificationLevel, _: () -> Unit ->
                when (tabConfig) {
                    NotificationLevel.Zero -> createLevelComponent(
                        context = context,
                        level = NotificationLevel.Zero,
                        items = NotificationItem.entries
                    )

                    NotificationLevel.One -> createLevelComponent(
                        context = context,
                        level = NotificationLevel.One,
                        items = listOf(NotificationItem.Declaration)
                    )

                    NotificationLevel.Two -> createLevelComponent(
                        context = context,
                        level = NotificationLevel.Two,
                        items = listOf(NotificationItem.Declaration)
                    )

                }

            },
        )
    @OptIn(ExperimentalCoroutinesApi::class)
    val notificationsForDrawer: StateFlow<List<NotificationDrawerUi>> = tabNavigationComponent.tabChildren.map {
         it.items.map { child->
            child.instance.countNotification.map { count->
                NotificationDrawerUi(
                    child.instance.level,
                    count
                )
            }
        }
    }.toStateFlow(lifecycle).flatMapLatest {flowList->
        combine(flowList){
            it.toList()
        }

    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        emptyList()
    )




    private fun createLevelComponent(
        context: ComponentContext,
        level: NotificationLevel,
        items: List<NotificationItem>,
    ): LevelNotificationComponent {
        return LevelNotificationComponent(
            componentContext = context,
            level = level,
            items = items,
            onOpenTab = onOpenTab,
            scope = scope
        )
    }

}

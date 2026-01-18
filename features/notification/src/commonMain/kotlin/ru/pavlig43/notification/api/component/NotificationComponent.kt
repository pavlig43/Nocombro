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
import ru.pavlig43.notification.api.model.NotificationDrawerUi
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.component.NotificationLevelComponent
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.di.createNotificationModule

/**
 *
 */
class NotificationComponent(
    componentContext: ComponentContext,
    private val onOpenTab: (NotificationItem, Int) -> Unit,
    dependencies: NotificationDependencies,
) : ComponentContext by componentContext, MainTabComponent {
    private val coroutineScope = componentCoroutineScope()
    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Оповещения"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(
        createNotificationModule(dependencies)
    )

    private val allRepositories = scope.getAll<INotificationRepository>()

    internal val tabNavigationComponent: TabNavigationComponent<NotificationTabConfig, NotificationTabChild> =
        TabNavigationComponent(
            componentContext = childContext("notification_tabs"),
            startConfigurations = listOf(
                NotificationTabConfig.Zero,
                NotificationTabConfig.One,
                NotificationTabConfig.Two,
            ),
            serializer = NotificationTabConfig.serializer(),
            tabChildFactory = { context: ComponentContext, tabChildConfig: NotificationTabConfig, _: () -> Unit ->
                NotificationTabChild(
                    createLevelComponent(
                        context,
                        tabChildConfig.notificationLevel
                    )
                )
            }
        )

    /**
     * Вкладки 3, но для каждой один компонент, который зависит от уровня важности
     *
     */
    private fun createLevelComponent(
        context: ComponentContext,
        level: NotificationLevel,
    ): NotificationLevelComponent {
        return NotificationLevelComponent(
            componentContext = context,
            level = level,
            onOpenTab = onOpenTab,
            repositoryList = allRepositories.filter { it.notificationLevel == level }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notificationsForDrawer: StateFlow<List<NotificationDrawerUi>> =
        tabNavigationComponent.tabChildren.map {
            it.items.map { child ->
                child.instance.component.countNotification.map { count ->
                    NotificationDrawerUi(
                        child.instance.component.level,
                        count
                    )
                }
            }
        }.toStateFlow(lifecycle)
            .flatMapLatest { flowList ->
            combine(flowList) {
                it.toList()
            }

        }
            .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            emptyList()
        )


}

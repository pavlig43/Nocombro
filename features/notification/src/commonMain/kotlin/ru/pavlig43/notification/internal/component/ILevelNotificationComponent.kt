package ru.pavlig43.notification.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.scope.Scope
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.di.with

internal interface ILevelNotificationComponent {
    val itemNotifications: List<INotificationItemComponent>
    val countNotification: StateFlow<Int>
    val level:NotificationLevel

}

internal class LevelNotificationComponent(
    componentContext: ComponentContext,
    override val level: NotificationLevel,
    items:List<NotificationItem>,
    onOpenTab: (NotificationItem, Int) -> Unit,
    scope: Scope
) : ComponentContext by componentContext, ILevelNotificationComponent {
    private val coroutineScope = componentCoroutineScope()

    private fun createNotificationLevelComponentList(
        level: NotificationLevel,
        items:List<NotificationItem>,
        onOpenTab: (NotificationItem, Int) -> Unit,
        scope: Scope,
    ): List<NotificationItemComponent> {
        return items.map { item->
            NotificationItemComponent(
                componentContext = childContext(item.name),
                repository = scope.get(level.with(item)),
                onOpenTab = onOpenTab,
                item = item,
                level = level
            )
        }

    }
    override val itemNotifications: List<INotificationItemComponent> =
        createNotificationLevelComponentList(
            level = level,
            items = items,
            onOpenTab = onOpenTab,
            scope = scope
        )
    override val countNotification: StateFlow<Int> = combine(
        itemNotifications.map { it.countNotification }
    ) { it.sum() }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        0
    )
}


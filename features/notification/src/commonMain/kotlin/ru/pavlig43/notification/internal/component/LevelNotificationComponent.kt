package ru.pavlig43.notification.internal.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.data.getNotificationBlock

/**
 * Содержит все уведомления с конкретным [NotificationLevel]
 * @param [level] уровень важности уведомления
 * @param [onOpenTab] колбэк на открытия вкладки с конкретным объектом у которого проблема
 * @param [repositoryList] список репозиториев которые дают поток всех уведомлений
 */
internal class NotificationLevelComponent(
    componentContext: ComponentContext,
    val level: NotificationLevel,
    private val onOpenTab: (NotificationItem, Int) -> Unit,
    repositoryList: List<INotificationRepository>
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    val notificationFlow = combine(
        repositoryList.map { it.getNotificationBlock() }
    ) { arrayList ->
        require(arrayList.all { it.level == level }) {
            "В списке репозиториев затесался не с тем уровнем важности NotificationLevel"
        }
        arrayList.toList()
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        emptyList()
    )
    val countNotification = notificationFlow.map { it.size }
    fun onClickItem(item: NotificationItem, id: Int) {
        onOpenTab(item, id)
    }
}


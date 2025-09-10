package ru.pavlig43.notification.internal.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.data.INotificationRepository
import ru.pavlig43.notification.internal.data.NotificationUi


internal class NotificationItemComponent(
    componentContext: ComponentContext,
    repository: INotificationRepository,
    override val onOpenTab: (item: NotificationItem, id: Int) -> Unit,
    override val item: NotificationItem,
    override val level: NotificationLevel,
):ComponentContext by componentContext, INotificationItemComponent {
    private val coroutineScope = componentCoroutineScope()

    override val notificationFlow: StateFlow<List<NotificationUi>> = repository.notificationFlow
    .stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        emptyList()
    )
    override val countNotification: StateFlow<Int> = notificationFlow.map {
        it.size
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        0
    )

    override fun onClickItem(id: Int) {
        onOpenTab(item,id)
    }



}
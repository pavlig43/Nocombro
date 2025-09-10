package ru.pavlig43.notification.internal.component

import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.notification.api.data.NotificationLevel
import ru.pavlig43.notification.internal.data.NotificationUi

internal interface INotificationItemComponent {
    val item:NotificationItem
    val level: NotificationLevel
    val notificationFlow:StateFlow<List<NotificationUi>>
    val countNotification:StateFlow<Int>
    fun onClickItem(id:Int)
    val onOpenTab:(item:NotificationItem,id:Int)->Unit
}
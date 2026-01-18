package ru.pavlig43.notification.internal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.api.model.NotificationLevel
import ru.pavlig43.notification.internal.model.NotificationBlockUi
import ru.pavlig43.notification.internal.model.NotificationUi

internal interface INotificationRepository {

    val notificationLevel: NotificationLevel
    val notificationItem: NotificationItem
    val mergedFromDBNotificationFlow: Flow<List<NotificationUi>>
}
internal fun INotificationRepository.getNotificationBlock(): Flow<NotificationBlockUi> {
    return mergedFromDBNotificationFlow.map {
        NotificationBlockUi(
            level = notificationLevel,
            item = notificationItem,
            notificationList = it
        )
    }
}


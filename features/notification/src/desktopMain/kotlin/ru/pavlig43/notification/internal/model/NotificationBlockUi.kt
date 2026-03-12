package ru.pavlig43.notification.internal.model

import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.notification.api.model.NotificationLevel

/**
 * Для отображения блока уведомлений для каждого [NotificationItem] с конкретным [NotificationLevel]
 */
internal data class NotificationBlockUi(
    val level: NotificationLevel,
    val item: NotificationItem,
    val notificationList: List<NotificationUi>
)
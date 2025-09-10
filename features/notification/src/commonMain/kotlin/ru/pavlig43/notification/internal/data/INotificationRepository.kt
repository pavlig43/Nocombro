package ru.pavlig43.notification.internal.data

import kotlinx.coroutines.flow.Flow

internal interface INotificationRepository {
    val notificationFlow: Flow<List<NotificationUi>>
}
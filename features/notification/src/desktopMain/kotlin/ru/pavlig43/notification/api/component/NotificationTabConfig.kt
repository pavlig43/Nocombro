package ru.pavlig43.notification.api.component

import kotlinx.serialization.Serializable
import ru.pavlig43.notification.api.model.NotificationLevel

@Serializable
internal sealed class NotificationTabConfig(
    val notificationLevel: NotificationLevel
){
    @Serializable
    data object Zero: NotificationTabConfig(NotificationLevel.HIGH)
    @Serializable
    data object One: NotificationTabConfig(NotificationLevel.MEDIUM)
    @Serializable
    data object Two: NotificationTabConfig(NotificationLevel.LOW)
}
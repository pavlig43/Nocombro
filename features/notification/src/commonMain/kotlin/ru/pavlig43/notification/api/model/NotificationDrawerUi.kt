package ru.pavlig43.notification.api.model


/**
 * Модель данных для отображения в боковой шторке.
 *
 * Содержит информацию об уровне и количестве уведомлений.
 */
data class NotificationDrawerUi(
    val level: NotificationLevel,
    val count: Int,
)
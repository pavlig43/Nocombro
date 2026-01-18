package ru.pavlig43.notification.internal.model

/**
 * @param id - id объекта в базе данных, нужен для открытия вкладки с этим объектом
 * @param text - текст уведомления
 */
internal data class NotificationUi(
    val id:Int,
    val text:String
)
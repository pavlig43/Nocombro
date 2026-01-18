package ru.pavlig43.notification.api.model


/**
 * Тип уведомлений.
 *
 * Каждый тип соответствует определенной бизнес-сущности и имеет(Продукт, Документ)
 *
 * @property title Локализованное название типа для отображения в UI
 */
enum class NotificationItem(val title: String) {
    Document("Документы"),
    Product("Продукты"),
    Declaration("Декларации"),

}
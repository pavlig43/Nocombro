package ru.pavlig43.database.data.transaction.reminder

import kotlinx.datetime.LocalDateTime

/**
 * DTO для передачи напоминаний в систему уведомлений
 */
data class ReminderNotificationDTO(
    val id: Int,
    val transactionId: Int,
    val text: String,
    val reminderDateTime: LocalDateTime
)

package ru.pavlig43.database.data.transaction.reminder

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject

data class ReminderBD(
    val text: String,
    val reminderDateTime: LocalDateTime,
    override val id: Int
) : CollectionObject

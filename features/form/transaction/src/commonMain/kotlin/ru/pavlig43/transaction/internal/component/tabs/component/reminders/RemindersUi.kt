package ru.pavlig43.transaction.internal.component.tabs.component.reminders

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class RemindersUi(
    override val composeId: Int,
    val id: Int,
    val transactionId: Int,
    val text: String,
    val reminderDateTime: LocalDateTime
) : IMultiLineTableUi

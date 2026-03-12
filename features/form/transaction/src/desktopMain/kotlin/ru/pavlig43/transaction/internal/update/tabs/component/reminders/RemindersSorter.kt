package ru.pavlig43.transaction.internal.update.tabs.component.reminders

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object RemindersSorter : SortMatcher<RemindersUi, RemindersField> {
    override fun sort(
        items: List<RemindersUi>,
        sort: SortState<RemindersField>?
    ): List<RemindersUi> {
        if (sort == null) return items

        val sortedList = when (sort.column) {
            RemindersField.TEXT -> items.sortedBy { it.text.lowercase() }
            RemindersField.REMINDER_DATE_TIME -> items.sortedBy { it.reminderDateTime }
            else -> items
        }

        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}

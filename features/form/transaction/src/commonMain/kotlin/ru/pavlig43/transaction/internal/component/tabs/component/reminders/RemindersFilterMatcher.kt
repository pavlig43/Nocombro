package ru.pavlig43.transaction.internal.component.tabs.component.reminders

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object RemindersFilterMatcher : FilterMatcher<RemindersUi, RemindersField>() {
    override fun matchesRules(
        item: RemindersUi,
        column: RemindersField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            RemindersField.SELECTION -> true
            RemindersField.COMPOSE_ID -> true
            RemindersField.TEXT -> matchesTextField(item.text, stateAny)
            RemindersField.REMINDER_DATE_TIME -> true 
        }
    }
}

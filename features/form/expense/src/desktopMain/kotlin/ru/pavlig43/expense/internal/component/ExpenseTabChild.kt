package ru.pavlig43.expense.internal.component

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.expense.internal.component.tabs.table.TableComponent

internal sealed interface ExpenseTabChild : FormTabChild {
    class Expenses(override val component: TableComponent) : ExpenseTabChild
}

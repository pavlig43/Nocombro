package ru.pavlig43.expense.internal.update

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.expense.internal.update.tabs.files.ExpenseFilesComponent
import ru.pavlig43.expense.internal.update.tabs.essential.ExpenseUpdateSingleLineComponent

internal sealed interface ExpenseTabChild : FormTabChild {
    class Essentials(override val component: ExpenseUpdateSingleLineComponent) : ExpenseTabChild
    class Files(override val component: ExpenseFilesComponent) : ExpenseTabChild
}

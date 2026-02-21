package ru.pavlig43.transaction.internal.update

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.transaction.internal.update.tabs.component.buy.BuyComponent
import ru.pavlig43.transaction.internal.update.tabs.component.expenses.ExpensesComponent
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf.PfComponent
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingridients.IngredientComponent
import ru.pavlig43.transaction.internal.update.tabs.component.reminders.RemindersComponent
import ru.pavlig43.transaction.internal.update.tabs.essential.TransactionUpdateSingleLineComponent

internal sealed interface TransactionTabChild : FormTabChild {
    class Essentials(override val component: TransactionUpdateSingleLineComponent) : TransactionTabChild

    class Buy(override val component: BuyComponent) : TransactionTabChild

    class Reminders(override val component: RemindersComponent) : TransactionTabChild

    class Expenses(override val component: ExpensesComponent) : TransactionTabChild

    class Pf(override val component: PfComponent) : TransactionTabChild

    class Ingredients(override val component: IngredientComponent) : TransactionTabChild
}

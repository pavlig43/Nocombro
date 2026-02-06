package ru.pavlig43.transaction.internal.component.tabs

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.transaction.internal.component.tabs.component.TransactionEssentialComponent
import ru.pavlig43.transaction.internal.component.tabs.component.buy.BuyComponent
import ru.pavlig43.transaction.internal.component.tabs.component.reminders.RemindersComponent

internal sealed interface TransactionTabChild: FormTabChild {
    class Essentials(override val component: TransactionEssentialComponent): TransactionTabChild

    class Buy(override val component: BuyComponent): TransactionTabChild

    class Reminders(override val component: RemindersComponent): TransactionTabChild
}
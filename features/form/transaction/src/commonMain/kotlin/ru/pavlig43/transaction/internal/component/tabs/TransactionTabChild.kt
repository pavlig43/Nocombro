package ru.pavlig43.transaction.internal.component.tabs

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.transaction.internal.component.tabs.component.TransactionEssentialComponent
import ru.pavlig43.transaction.internal.component.tabs.component.buy.BuyComponent

internal sealed interface TransactionTabChild: FormTabChild {
    class Essentials(override val component: TransactionEssentialComponent): TransactionTabChild

    class Buy(override val component: BuyComponent): TransactionTabChild
}
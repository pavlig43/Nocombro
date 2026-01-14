package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy

import ru.pavlig43.core.FormTabChild

internal sealed interface BuyTabChild: FormTabChild{
    class Essentials(override val component: BuyEssentialComponent): BuyTabChild
}
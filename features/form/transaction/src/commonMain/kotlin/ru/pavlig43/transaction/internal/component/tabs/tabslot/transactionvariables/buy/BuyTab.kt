package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface BuyTab {
    @Serializable
    data object Essentials: BuyTab

//    @Serializable
//    data object BaseProduct: BuyTab

}

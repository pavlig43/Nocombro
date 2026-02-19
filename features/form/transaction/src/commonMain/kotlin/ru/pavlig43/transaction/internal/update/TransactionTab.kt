package ru.pavlig43.transaction.internal.update

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface TransactionTab {
    @Serializable
    data object Essentials : TransactionTab

    @Serializable
    data object Buy : TransactionTab

    @Serializable
    data object Reminders : TransactionTab

    @Serializable
    data object Expenses : TransactionTab

    @Serializable
    data object Pf : TransactionTab
}

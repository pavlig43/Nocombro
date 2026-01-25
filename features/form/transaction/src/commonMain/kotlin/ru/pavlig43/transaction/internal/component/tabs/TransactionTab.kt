package ru.pavlig43.transaction.internal.component.tabs

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface TransactionTab {
    @Serializable
    data object Essentials: TransactionTab

    @Serializable
    data object Buy: TransactionTab
}
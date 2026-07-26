package ru.pavlig43.immutable.internal.component.items.transaction

import ru.pavlig43.database.data.transact.Transact

internal data class TransactionTableItem(
    val transaction: Transact,
    val counterpartyNames: String,
)

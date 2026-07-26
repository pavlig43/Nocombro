package ru.pavlig43.database.data.transact

import androidx.room.ColumnInfo

data class TransactionCounterpartyName(
    @ColumnInfo("transaction_id")
    val transactionId: Int,

    @ColumnInfo("counterparty_name")
    val counterpartyName: String,
)

package ru.pavlig43.itemlist.internal.component.items.transaction

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.itemlist.api.model.ITableUi

data class TransactionTableUi(

    val transactionType: TransactionType,


    val createdAt: LocalDateTime,

    val comment: String,

    val isCompleted: Boolean,

    override val composeId: Int = 0,


    ) : ITableUi
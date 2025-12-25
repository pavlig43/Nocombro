package ru.pavlig43.itemlist.internal.component.items.transaction

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.transaction.MovementType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.itemlist.api.model.IItemUi

data class TransactionItemUi(

    val transactionType: TransactionType,


    val createdAt: LocalDateTime,

    val comment: String,

    val isCompleted: Boolean,

    override val id: Int = 0,


    ) : IItemUi
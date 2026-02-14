package ru.pavlig43.immutable.internal.component.items.transaction

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.emptyLocalDateTime
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class TransactionTableUi(

    val transactionType: TransactionType = TransactionType.BUY,

    val createdAt: LocalDateTime = emptyLocalDateTime,

    val comment: String = "",

    val isCompleted: Boolean = true,

    override val composeId: Int = 0,


    ) : IMultiLineTableUi
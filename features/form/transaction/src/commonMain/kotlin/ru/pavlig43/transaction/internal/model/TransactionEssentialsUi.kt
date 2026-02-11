package ru.pavlig43.transaction.internal.model

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.getCurrentLocalDateTime
import ru.pavlig43.core.model.ItemEssentialsUi
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.tablecore.model.ISingleLineTableUi
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal data class TransactionEssentialsUi(
    val transactionType: TransactionType? = null,

    val createdAt: LocalDateTime = getCurrentLocalDateTime(),

    val comment: String ="",

    val isCompleted: Boolean = true,

    override val id: Int = 0,

    ) : ItemEssentialsUi, ISingleLineTableUi


internal fun Transaction.toUi(): TransactionEssentialsUi {
    return TransactionEssentialsUi(
        createdAt = createdAt,
        comment = comment,
        id = id,
        transactionType = transactionType,
        isCompleted = isCompleted
    )
}


internal fun TransactionEssentialsUi.toDto(): Transaction {
    return Transaction(
        transactionType = transactionType?:throw IllegalArgumentException("transaction type required"),
        createdAt = createdAt,
        comment = comment,
        isCompleted = isCompleted,
        id = id
    )
}

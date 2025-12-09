package ru.pavlig43.transaction.internal.data

import ru.pavlig43.core.data.ItemEssentialsUi
import ru.pavlig43.core.getUTCNow
import ru.pavlig43.database.data.transaction.OperationType
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.database.data.transaction.TransactionType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal data class TransactionEssentialsUi(
    val transactionType: TransactionType? = null,

    val operationType: OperationType? = null,

    val createdAt: Long? = getUTCNow(),

    val comment: String ="",

    val isCompleted: Boolean = false,

    override val id: Int = 0,

    ) : ItemEssentialsUi


internal fun ProductTransaction.toUi(): TransactionEssentialsUi {
    return TransactionEssentialsUi(
        createdAt = createdAt,
        comment = comment,
        id = id,
        transactionType = transactionType,
        operationType = operationType,
        isCompleted = isCompleted
    )
}


internal fun TransactionEssentialsUi.toDto(): ProductTransaction {
    return ProductTransaction(
        transactionType = transactionType?:throw IllegalArgumentException("transaction type required"),
        operationType = operationType?:throw IllegalArgumentException("operation type required"),
        createdAt = createdAt?:throw IllegalArgumentException("date require not null"),
        comment = comment,
        isCompleted = isCompleted,
        id = id
    )
}

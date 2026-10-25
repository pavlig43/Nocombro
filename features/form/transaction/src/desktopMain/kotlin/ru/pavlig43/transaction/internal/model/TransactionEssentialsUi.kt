package ru.pavlig43.transaction.internal.model

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.getCurrentLocalDateTime
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal data class TransactionEssentialsUi(
    val transactionType: TransactionType? = null,

    val createdAt: LocalDateTime = getCurrentLocalDateTime(),

    val commentState: TextFieldState = TextFieldState(),

    val isCompleted: Boolean = true,

    val id: Int = 0,

    val syncId: String = defaultSyncId(),

    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    val deletedAt: LocalDateTime? = null,

) : SingleLineInputState {
    val comment: String get() = commentState.text.toString()
    override val textFields get() = listOf(commentState)
}


internal fun Transact.toUi(): TransactionEssentialsUi {
    return TransactionEssentialsUi(
        createdAt = createdAt,
        commentState = TextFieldState(comment),
        id = id,
        transactionType = transactionType,
        isCompleted = isCompleted,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}


internal fun TransactionEssentialsUi.toDto(): Transact {
    return Transact(
        transactionType = transactionType?:throw IllegalArgumentException("transaction type required"),
        createdAt = createdAt,
        comment = comment,
        isCompleted = isCompleted,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

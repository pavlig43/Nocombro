package ru.pavlig43.storage.internal.model

import ru.pavlig43.database.data.storage.StorageBatch

internal data class StorageBatchUi(
    val batchId: Int,
    val batchName: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
)

internal fun StorageBatch.toUi(): StorageBatchUi {
    return StorageBatchUi(
        batchId = batchId,
        batchName = batchName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd
    )
}

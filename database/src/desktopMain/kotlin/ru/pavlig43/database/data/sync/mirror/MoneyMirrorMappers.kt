package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.money.MoneyAccount
import ru.pavlig43.database.data.money.MoneyMovement

/** Converts a local money account to its stable typed mirror form. */
internal fun MoneyAccount.toMirrorRow() = MoneyAccountMirrorRow(
    syncId = syncId,
    name = name,
    accountType = accountType,
    openedAt = openedAt,
    isArchived = isArchived,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** Replaces local account ids with stable sync ids before transport. */
internal suspend fun MoneyMovement.toMirrorRow(
    db: NocombroDatabase,
): MoneyMovementMirrorRow {
    val fromAccountSyncId = fromAccountId
        ?.let { requireNotNull(db.moneyDao.getAccount(it)).syncId }
    val toAccountSyncId = toAccountId
        ?.let { requireNotNull(db.moneyDao.getAccount(it)).syncId }
    return MoneyMovementMirrorRow(
        syncId = syncId,
        kind = kind,
        category = category,
        amount = amount,
        occurredAt = occurredAt,
        fromAccountSyncId = fromAccountSyncId,
        toAccountSyncId = toAccountSyncId,
        counterparty = counterparty,
        comment = comment,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

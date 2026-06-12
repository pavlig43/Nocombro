package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.sale.SaleBDIn

/** Преобразует движение партии и заменяет оба локальных FK на sync id. */
internal suspend fun BatchMovement.toMirrorRow(
    db: NocombroDatabase,
): BatchMovementMirrorRow {
    val batch = db.batchDao.getBatch(batchId)
    val transaction = db.transactionDao.getTransaction(transactionId)
    return BatchMovementMirrorRow(
        syncId = syncId,
        batchSyncId = batch.syncId,
        movementType = movementType,
        count = count,
        transactionSyncId = transaction.syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/** Преобразует напоминание транзакции со стабильной ссылкой на transaction. */
internal suspend fun ReminderBD.toMirrorRow(
    db: NocombroDatabase,
): ReminderMirrorRow {
    val transaction = db.transactionDao.getTransaction(transactionId)
    return ReminderMirrorRow(
        syncId = syncId,
        transactionSyncId = transaction.syncId,
        text = text,
        reminderDateTime = reminderDateTime,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/**
 * Преобразует расход; nullable transaction relation остается nullable и в mirror.
 */
internal suspend fun ExpenseBD.toMirrorRow(
    db: NocombroDatabase,
): ExpenseMirrorRow {
    val transactionSyncId = transactionId
        ?.let { db.transactionDao.getTransaction(it).syncId }
    return ExpenseMirrorRow(
        syncId = syncId,
        transactionSyncId = transactionSyncId,
        expenseType = expenseType,
        amount = amount,
        expenseDateTime = expenseDateTime,
        comment = comment,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/**
 * Преобразует покупку со ссылками на родительскую транзакцию и движение партии.
 */
internal suspend fun BuyBDIn.toMirrorRow(
    db: NocombroDatabase,
): BuyMirrorRow {
    val transaction = db.transactionDao.getTransaction(transactionId)
    val movement = db.batchMovementDao.getMovement(movementId)
    return BuyMirrorRow(
        syncId = syncId,
        transactionSyncId = transaction.syncId,
        movementSyncId = movement.syncId,
        price = price,
        comment = comment,
        ndsPercent = ndsPercent,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/**
 * Преобразует продажу и переносит transaction, movement и client через sync id.
 */
internal suspend fun SaleBDIn.toMirrorRow(
    db: NocombroDatabase,
): SaleMirrorRow {
    val transaction = db.transactionDao.getTransaction(transactionId)
    val movement = db.batchMovementDao.getMovement(movementId)
    val client = db.vendorDao.getVendor(clientId)
    return SaleMirrorRow(
        syncId = syncId,
        transactionSyncId = transaction.syncId,
        movementSyncId = movement.syncId,
        price = price,
        comment = comment,
        clientSyncId = client.syncId,
        ndsPercent = ndsPercent,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

package ru.pavlig43.database.data.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BATCH_MOVEMENT_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_TABLE_NAME
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.product.COMPOSITION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_DECLARATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.transact.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.transact.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transact.reminder.REMINDER_TABLE_NAME
import ru.pavlig43.database.data.transact.sale.SALE_TABLE_NAME
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME

class SyncEntityExportRepository(
    private val db: NocombroDatabase,
    private val json: Json = Json { encodeDefaults = true },
) {

    suspend fun export(change: SyncPushChange): RemotePushChange {
        val payloadJson = if (change.changeType == SyncChangeType.DELETE) {
            null
        } else {
            loadPayloadJson(change.entityTable, change.entityLocalId)
        }

        return RemotePushChange(
            entityTable = change.entityTable,
            entityLocalId = change.entityLocalId,
            changeType = change.changeType,
            sourceQueueIds = change.sourceQueueIds,
            lastQueuedAt = change.lastQueuedAt,
            payloadJson = payloadJson,
        )
    }

    private suspend fun loadPayloadJson(
        entityTable: String,
        entitySyncId: String,
    ): String? {
        return when (entityTable) {
            VENDOR_TABLE_NAME -> db.vendorDao.getVendorBySyncId(entitySyncId)?.let { vendor ->
                encodePayload(
                    VendorSyncPayload(
                        syncId = vendor.syncId,
                        displayName = vendor.displayName,
                        comment = vendor.comment,
                        updatedAt = vendor.updatedAt,
                        deletedAt = vendor.deletedAt,
                    )
                )
            }

            DOCUMENT_TABLE_NAME -> db.documentDao.getDocumentBySyncId(entitySyncId)?.let { document ->
                encodePayload(
                    DocumentSyncPayload(
                        syncId = document.syncId,
                        displayName = document.displayName,
                        type = document.type,
                        createdAt = document.createdAt,
                        comment = document.comment,
                        updatedAt = document.updatedAt,
                        deletedAt = document.deletedAt,
                    )
                )
            }

            DECLARATIONS_TABLE_NAME -> db.declarationDao.getDeclarationBySyncId(entitySyncId)?.let { declaration ->
                val vendor = db.vendorDao.getVendor(declaration.vendorId)
                encodePayload(
                    DeclarationSyncPayload(
                        syncId = declaration.syncId,
                        displayName = declaration.displayName,
                        createdAt = declaration.createdAt,
                        vendorSyncId = vendor.syncId,
                        vendorName = declaration.vendorName,
                        bornDate = declaration.bornDate,
                        bestBefore = declaration.bestBefore,
                        observeFromNotification = declaration.observeFromNotification,
                        updatedAt = declaration.updatedAt,
                        deletedAt = declaration.deletedAt,
                    )
                )
            }

            PRODUCT_TABLE_NAME -> db.productDao.getProductBySyncId(entitySyncId)?.let { product ->
                encodePayload(
                    ProductSyncPayload(
                        syncId = product.syncId,
                        type = product.type,
                        displayName = product.displayName,
                        secondName = product.secondName,
                        createdAt = product.createdAt,
                        comment = product.comment,
                        priceForSale = product.priceForSale,
                        shelfLifeDays = product.shelfLifeDays,
                        recNds = product.recNds,
                        updatedAt = product.updatedAt,
                        deletedAt = product.deletedAt,
                    )
                )
            }

            COMPOSITION_TABLE_NAME -> db.compositionDao.getCompositionBySyncId(entitySyncId)?.let { composition ->
                val parent = db.productDao.getProduct(composition.parentId)
                val product = db.productDao.getProduct(composition.productId)
                encodePayload(
                    CompositionSyncPayload(
                        syncId = composition.syncId,
                        parentSyncId = parent.syncId,
                        productSyncId = product.syncId,
                        count = composition.count,
                        updatedAt = composition.updatedAt,
                        deletedAt = composition.deletedAt,
                    )
                )
            }

            PRODUCT_DECLARATION_TABLE_NAME -> db.productDeclarationDao.getProductDeclarationBySyncId(entitySyncId)?.let { productDeclaration ->
                val product = db.productDao.getProduct(productDeclaration.productId)
                val declaration = db.declarationDao.getDeclaration(productDeclaration.declarationId)
                encodePayload(
                    ProductDeclarationSyncPayload(
                        syncId = productDeclaration.syncId,
                        productSyncId = product.syncId,
                        declarationSyncId = declaration.syncId,
                        isProductInDeclaration = productDeclaration.isProductInDeclaration,
                        updatedAt = productDeclaration.updatedAt,
                        deletedAt = productDeclaration.deletedAt,
                    )
                )
            }

            BATCH_TABLE_NAME -> db.batchDao.getBatchBySyncId(entitySyncId)?.let { batch ->
                val product = db.productDao.getProduct(batch.productId)
                val declaration = db.declarationDao.getDeclaration(batch.declarationId)
                encodePayload(
                    BatchSyncPayload(
                        syncId = batch.syncId,
                        productSyncId = product.syncId,
                        dateBorn = batch.dateBorn,
                        declarationSyncId = declaration.syncId,
                        updatedAt = batch.updatedAt,
                        deletedAt = batch.deletedAt,
                    )
                )
            }

            BATCH_MOVEMENT_TABLE_NAME -> db.batchMovementDao.getMovementBySyncId(entitySyncId)?.let { movement ->
                val batch = db.batchDao.getBatch(movement.batchId)
                val transaction = db.transactionDao.getTransaction(movement.transactionId)
                encodePayload(
                    BatchMovementSyncPayload(
                        syncId = movement.syncId,
                        batchSyncId = batch.syncId,
                        movementType = movement.movementType,
                        count = movement.count,
                        transactionSyncId = transaction.syncId,
                        updatedAt = movement.updatedAt,
                        deletedAt = movement.deletedAt,
                    )
                )
            }

            BUY_TABLE_NAME -> db.buyDao.getBuyBySyncId(entitySyncId)?.let { buy ->
                val transaction = db.transactionDao.getTransaction(buy.transactionId)
                val movement = db.batchMovementDao.getMovement(buy.movementId)
                encodePayload(
                    BuySyncPayload(
                        syncId = buy.syncId,
                        transactionSyncId = transaction.syncId,
                        movementSyncId = movement.syncId,
                        price = buy.price,
                        comment = buy.comment,
                        ndsPercent = buy.ndsPercent,
                        updatedAt = buy.updatedAt,
                        deletedAt = buy.deletedAt,
                    )
                )
            }

            SALE_TABLE_NAME -> db.saleDao.getSaleBySyncId(entitySyncId)?.let { sale ->
                val transaction = db.transactionDao.getTransaction(sale.transactionId)
                val movement = db.batchMovementDao.getMovement(sale.movementId)
                val client = db.vendorDao.getVendor(sale.clientId)
                encodePayload(
                    SaleSyncPayload(
                        syncId = sale.syncId,
                        transactionSyncId = transaction.syncId,
                        movementSyncId = movement.syncId,
                        price = sale.price,
                        comment = sale.comment,
                        clientSyncId = client.syncId,
                        ndsPercent = sale.ndsPercent,
                        updatedAt = sale.updatedAt,
                        deletedAt = sale.deletedAt,
                    )
                )
            }

            REMINDER_TABLE_NAME -> db.reminderDao.getReminderBySyncId(entitySyncId)?.let { reminder ->
                val transaction = db.transactionDao.getTransaction(reminder.transactionId)
                encodePayload(
                    ReminderSyncPayload(
                        syncId = reminder.syncId,
                        transactionSyncId = transaction.syncId,
                        text = reminder.text,
                        reminderDateTime = reminder.reminderDateTime,
                        updatedAt = reminder.updatedAt,
                        deletedAt = reminder.deletedAt,
                    )
                )
            }

            EXPENSE_TABLE_NAME -> db.expenseDao.getExpenseBySyncId(entitySyncId)?.let { expense ->
                val transactionSyncId = expense.transactionId
                    ?.let { db.transactionDao.getTransaction(it).syncId }
                encodePayload(
                    ExpenseSyncPayload(
                        syncId = expense.syncId,
                        transactionSyncId = transactionSyncId,
                        expenseType = expense.expenseType,
                        amount = expense.amount,
                        expenseDateTime = expense.expenseDateTime,
                        comment = expense.comment,
                        updatedAt = expense.updatedAt,
                        deletedAt = expense.deletedAt,
                    )
                )
            }

            TRANSACTION_TABLE_NAME -> db.transactionDao.getTransactionBySyncId(entitySyncId)?.let { transaction ->
                encodePayload(
                    TransactionSyncPayload(
                        syncId = transaction.syncId,
                        transactionType = transaction.transactionType,
                        createdAt = transaction.createdAt,
                        comment = transaction.comment,
                        isCompleted = transaction.isCompleted,
                        updatedAt = transaction.updatedAt,
                        deletedAt = transaction.deletedAt,
                    )
                )
            }

            else -> null
        }
    }

    private inline fun <reified T> encodePayload(
        payload: T,
    ): String {
        return json.encodeToString(
            SyncPayloadEnvelope(payload = payload)
        )
    }
}

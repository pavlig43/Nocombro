package ru.pavlig43.database.data.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BATCH_MOVEMENT_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_TABLE_NAME
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.product.COMPOSITION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_DECLARATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_SPECIFICATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.SAFETY_STOCK_TABLE_NAME
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
        val reminderEmailSource = loadReminderEmailSourceChange(change)

        return RemotePushChange(
            entityTable = change.entityTable,
            entityLocalId = change.entityLocalId,
            changeType = change.changeType,
            sourceQueueIds = change.sourceQueueIds,
            lastQueuedAt = change.lastQueuedAt,
            payloadJson = payloadJson,
            reminderEmailSource = reminderEmailSource,
        )
    }

    private suspend fun loadReminderEmailSourceChange(
        change: SyncPushChange,
    ): ReminderEmailSourceChange? {
        if (change.entityTable != REMINDER_TABLE_NAME) {
            return null
        }

        if (change.changeType == SyncChangeType.DELETE) {
            return ReminderEmailSourceChange(
                reminderSyncId = change.entityLocalId,
                transactionSyncId = "",
                transactionType = "",
                transactionCreatedAt = change.lastQueuedAt,
                reminderText = null,
                reminderAt = null,
                updatedAt = change.lastQueuedAt,
                deletedAt = change.lastQueuedAt,
            )
        }

        val reminder = db.reminderDao.getReminderBySyncId(change.entityLocalId) ?: return null
        val transaction = db.transactionDao.getTransaction(reminder.transactionId)
        return ReminderEmailSourceChange(
            reminderSyncId = reminder.syncId,
            transactionSyncId = transaction.syncId,
            transactionType = transaction.transactionType.displayName,
            transactionCreatedAt = transaction.createdAt,
            reminderText = reminder.text,
            reminderAt = reminder.reminderDateTime,
            updatedAt = reminder.updatedAt,
            deletedAt = reminder.deletedAt,
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

            PRODUCT_SPECIFICATION_TABLE_NAME -> db.productSpecificationDao.getBySyncId(entitySyncId)?.let { specification ->
                val product = db.productDao.getProduct(specification.productId)
                encodePayload(
                    ProductSpecificationSyncPayload(
                        syncId = specification.syncId,
                        productSyncId = product.syncId,
                        description = specification.description,
                        dosage = specification.dosage,
                        composition = specification.composition,
                        shelfLifeText = specification.shelfLifeText,
                        storageConditions = specification.storageConditions,
                        appearance = specification.appearance,
                        color = specification.color,
                        smell = specification.smell,
                        taste = specification.taste,
                        physicalChemicalIndicators = specification.physicalChemicalIndicators,
                        microbiologicalIndicators = specification.microbiologicalIndicators,
                        toxicElements = specification.toxicElements,
                        allergens = specification.allergens,
                        gmoInfo = specification.gmoInfo,
                        updatedAt = specification.updatedAt,
                        deletedAt = specification.deletedAt,
                    )
                )
            }

            SAFETY_STOCK_TABLE_NAME -> db.safetyStockDao.getBySyncId(entitySyncId)?.let { safetyStock ->
                val product = db.productDao.getProduct(safetyStock.productId)
                encodePayload(
                    SafetyStockSyncPayload(
                        syncId = safetyStock.syncId,
                        productSyncId = product.syncId,
                        reorderPoint = safetyStock.reorderPoint,
                        orderQuantity = safetyStock.orderQuantity,
                        updatedAt = safetyStock.updatedAt,
                        deletedAt = safetyStock.deletedAt,
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

            FILE_TABLE_NAME -> db.fileDao.getFileBySyncId(entitySyncId)?.let { file ->
                encodePayload(
                    FileSyncPayload(
                        syncId = file.syncId,
                        ownerType = file.ownerFileType,
                        ownerSyncId = loadOwnerSyncId(
                            ownerId = file.ownerId,
                            ownerType = file.ownerFileType,
                        ),
                        displayName = file.displayName,
                        path = file.path,
                        remoteObjectKey = file.remoteObjectKey,
                        remoteStorageProvider = file.remoteStorageProvider,
                        updatedAt = file.updatedAt,
                        deletedAt = file.deletedAt,
                    )
                )
            }

            else -> null
        }
    }

    private suspend fun loadOwnerSyncId(
        ownerId: Int,
        ownerType: OwnerType,
    ): String {
        return when (ownerType) {
            OwnerType.DECLARATION -> db.declarationDao.getDeclaration(ownerId).syncId
            OwnerType.PRODUCT -> db.productDao.getProduct(ownerId).syncId
            OwnerType.VENDOR -> db.vendorDao.getVendor(ownerId).syncId
            OwnerType.DOCUMENT -> db.documentDao.getDocument(ownerId).syncId
            OwnerType.TRANSACTION -> db.transactionDao.getTransaction(ownerId).syncId
            OwnerType.EXPENSE -> db.expenseDao.getExpense(ownerId)?.syncId
                ?: error("Missing expense owner for id=$ownerId")
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

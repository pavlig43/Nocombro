package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BATCH_MOVEMENT_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_TABLE_NAME
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.buildCanonicalFileKey
import ru.pavlig43.database.data.files.buildManagedLocalFilePath
import ru.pavlig43.database.data.files.extractFileName
import ru.pavlig43.database.data.product.COMPOSITION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_DECLARATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.SAFETY_STOCK_TABLE_NAME
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.database.data.transact.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.reminder.REMINDER_TABLE_NAME
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.sale.SALE_TABLE_NAME
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.inTransaction

class SyncRemoteApplyRepository(
    private val db: NocombroDatabase,
    private val json: Json = Json,
) {

    suspend fun applyChanges(
        changes: List<RemotePullChange>,
    ) {
        if (changes.isEmpty()) {
            return
        }

        db.inTransaction {
            val sortedChanges = changes
                .sortedWith(compareBy<RemotePullChange>({ entityPriority(it.entityTable) }, { it.cursor }))
            for (change in sortedChanges) {
                applySingleChange(change)
            }
        }
    }

    private suspend fun applySingleChange(change: RemotePullChange) {
        when (change.entityTable) {
            VENDOR_TABLE_NAME -> applyVendor(change)
            DOCUMENT_TABLE_NAME -> applyDocument(change)
            PRODUCT_TABLE_NAME -> applyProduct(change)
            SAFETY_STOCK_TABLE_NAME -> applySafetyStock(change)
            TRANSACTION_TABLE_NAME -> applyTransaction(change)
            DECLARATIONS_TABLE_NAME -> applyDeclaration(change)
            BATCH_TABLE_NAME -> applyBatch(change)
            PRODUCT_DECLARATION_TABLE_NAME -> applyProductDeclaration(change)
            COMPOSITION_TABLE_NAME -> applyComposition(change)
            BATCH_MOVEMENT_TABLE_NAME -> applyBatchMovement(change)
            REMINDER_TABLE_NAME -> applyReminder(change)
            EXPENSE_TABLE_NAME -> applyExpense(change)
            BUY_TABLE_NAME -> applyBuy(change)
            SALE_TABLE_NAME -> applySale(change)
            FILE_TABLE_NAME -> applyFile(change)
        }
    }

    private suspend fun applyVendor(change: RemotePullChange) {
        val existing = db.vendorDao.getVendorBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.vendorDao.updateVendor(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<VendorSyncPayload>()
        val incoming = Vendor(
            displayName = payload.displayName,
            comment = payload.comment,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null) {
            if (isStale(existing.updatedAt, incoming.updatedAt)) return
            db.vendorDao.updateVendor(incoming)
        } else {
            db.vendorDao.create(incoming)
        }
    }

    private suspend fun applyDocument(change: RemotePullChange) {
        val existing = db.documentDao.getDocumentBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.documentDao.updateDocument(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<DocumentSyncPayload>()
        val incoming = Document(
            displayName = payload.displayName,
            type = payload.type,
            createdAt = payload.createdAt,
            comment = payload.comment,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null) {
            if (isStale(existing.updatedAt, incoming.updatedAt)) return
            db.documentDao.updateDocument(incoming)
        } else {
            db.documentDao.create(incoming)
        }
    }

    private suspend fun applyProduct(change: RemotePullChange) {
        val existing = db.productDao.getProductBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.productDao.updateProduct(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<ProductSyncPayload>()
        val incoming = Product(
            type = payload.type,
            displayName = payload.displayName,
            secondName = payload.secondName,
            createdAt = payload.createdAt,
            comment = payload.comment,
            priceForSale = payload.priceForSale,
            shelfLifeDays = payload.shelfLifeDays,
            recNds = payload.recNds,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null) {
            if (isStale(existing.updatedAt, incoming.updatedAt)) return
            db.productDao.updateProduct(incoming)
        } else {
            db.productDao.create(incoming)
        }
    }

    private suspend fun applySafetyStock(change: RemotePullChange) {
        val existing = db.safetyStockDao.getBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.safetyStockDao.upsert(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<SafetyStockSyncPayload>()
        val product = requireProduct(payload.productSyncId)
        val incoming = SafetyStock(
            productId = product.id,
            reorderPoint = payload.reorderPoint,
            orderQuantity = payload.orderQuantity,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.safetyStockDao.upsert(incoming)
    }

    private suspend fun applyTransaction(change: RemotePullChange) {
        val existing = db.transactionDao.getTransactionBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.transactionDao.updateTransaction(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<TransactionSyncPayload>()
        val incoming = Transact(
            transactionType = payload.transactionType,
            createdAt = payload.createdAt,
            comment = payload.comment,
            isCompleted = payload.isCompleted,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null) {
            if (isStale(existing.updatedAt, incoming.updatedAt)) return
            db.transactionDao.updateTransaction(incoming)
        } else {
            db.transactionDao.create(incoming)
        }
    }

    private suspend fun applyDeclaration(change: RemotePullChange) {
        val existing = db.declarationDao.getDeclarationBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.declarationDao.updateDeclaration(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<DeclarationSyncPayload>()
        val vendor = requireVendor(payload.vendorSyncId)
        val incoming = Declaration(
            displayName = payload.displayName,
            createdAt = payload.createdAt,
            vendorId = vendor.id,
            vendorName = vendor.displayName,
            bornDate = payload.bornDate,
            bestBefore = payload.bestBefore,
            observeFromNotification = payload.observeFromNotification,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null) {
            if (isStale(existing.updatedAt, incoming.updatedAt)) return
            db.declarationDao.updateDeclaration(incoming)
        } else {
            db.declarationDao.create(incoming)
        }
    }

    private suspend fun applyBatch(change: RemotePullChange) {
        val existing = db.batchDao.getBatchBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.batchDao.updateBatch(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<BatchSyncPayload>()
        val product = requireProduct(payload.productSyncId)
        val declaration = requireDeclaration(payload.declarationSyncId)
        val incoming = BatchBD(
            id = existing?.id ?: 0,
            productId = product.id,
            dateBorn = payload.dateBorn,
            declarationId = declaration.id,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null) {
            if (isStale(existing.updatedAt, incoming.updatedAt)) return
            db.batchDao.updateBatch(incoming)
        } else {
            db.batchDao.createBatch(incoming)
        }
    }

    private suspend fun applyProductDeclaration(change: RemotePullChange) {
        val existing = db.productDeclarationDao.getProductDeclarationBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.productDeclarationDao.upsertProductDeclarations(
                        listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                    )
                }
            }
            return
        }

        val payload = change.decodePayload<ProductDeclarationSyncPayload>()
        val product = requireProduct(payload.productSyncId)
        val declaration = requireDeclaration(payload.declarationSyncId)
        val incoming = ProductDeclarationIn(
            productId = product.id,
            declarationId = declaration.id,
            isProductInDeclaration = payload.isProductInDeclaration,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.productDeclarationDao.upsertProductDeclarations(listOf(incoming))
    }

    private suspend fun applyComposition(change: RemotePullChange) {
        val existing = db.compositionDao.getCompositionBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.compositionDao.upsertComposition(listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt)))
                }
            }
            return
        }

        val payload = change.decodePayload<CompositionSyncPayload>()
        val parent = requireProduct(payload.parentSyncId)
        val product = requireProduct(payload.productSyncId)
        val incoming = CompositionIn(
            id = existing?.id ?: 0,
            parentId = parent.id,
            productId = product.id,
            count = payload.count,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.compositionDao.upsertComposition(listOf(incoming))
    }

    private suspend fun applyBatchMovement(change: RemotePullChange) {
        val existing = db.batchMovementDao.getMovementBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.batchMovementDao.upsertMovement(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<BatchMovementSyncPayload>()
        val batch = requireBatch(payload.batchSyncId)
        val transaction = requireTransaction(payload.transactionSyncId)
        val incoming = BatchMovement(
            batchId = batch.id,
            movementType = payload.movementType,
            count = payload.count,
            transactionId = transaction.id,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.batchMovementDao.upsertMovement(incoming)
    }

    private suspend fun applyReminder(change: RemotePullChange) {
        val existing = db.reminderDao.getReminderBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.reminderDao.upsertAll(listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt)))
                }
            }
            return
        }

        val payload = change.decodePayload<ReminderSyncPayload>()
        val transaction = requireTransaction(payload.transactionSyncId)
        val incoming = ReminderBD(
            transactionId = transaction.id,
            text = payload.text,
            reminderDateTime = payload.reminderDateTime,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.reminderDao.upsertAll(listOf(incoming))
    }

    private suspend fun applyExpense(change: RemotePullChange) {
        val existing = db.expenseDao.getExpenseBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.expenseDao.updateExpense(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<ExpenseSyncPayload>()
        val transactionId = payload.transactionSyncId?.let { requireTransaction(it).id }
        val incoming = ExpenseBD(
            transactionId = transactionId,
            expenseType = payload.expenseType,
            amount = payload.amount,
            expenseDateTime = payload.expenseDateTime,
            comment = payload.comment,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null) {
            if (isStale(existing.updatedAt, incoming.updatedAt)) return
            db.expenseDao.updateExpense(incoming)
        } else {
            db.expenseDao.insertExpense(incoming)
        }
    }

    private suspend fun applyBuy(change: RemotePullChange) {
        val existing = db.buyDao.getBuyBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.buyDao.upsertBuyBd(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<BuySyncPayload>()
        val transaction = requireTransaction(payload.transactionSyncId)
        val movement = requireMovement(payload.movementSyncId)
        val incoming = BuyBDIn(
            transactionId = transaction.id,
            movementId = movement.id,
            price = payload.price,
            comment = payload.comment,
            ndsPercent = payload.ndsPercent,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.buyDao.upsertBuyBd(incoming)
    }

    private suspend fun applySale(change: RemotePullChange) {
        val existing = db.saleDao.getSaleBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.saleDao.upsertSaleBd(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.decodePayload<SaleSyncPayload>()
        val transaction = requireTransaction(payload.transactionSyncId)
        val movement = requireMovement(payload.movementSyncId)
        val client = requireVendor(payload.clientSyncId)
        val incoming = SaleBDIn(
            transactionId = transaction.id,
            movementId = movement.id,
            price = payload.price,
            comment = payload.comment,
            clientId = client.id,
            ndsPercent = payload.ndsPercent,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.saleDao.upsertSaleBd(incoming)
    }

    private suspend fun applyFile(change: RemotePullChange) {
        val existing = db.fileDao.getFileBySyncId(change.entitySyncId)
        if (change.changeType == SyncChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.fileDao.upsertFiles(
                        listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                    )
                }
            }
            return
        }

        val payload = change.decodePayload<FileSyncPayload>()
        val ownerId = requireOwnerId(
            ownerType = payload.ownerType,
            ownerSyncId = payload.ownerSyncId,
        )
        val incoming = FileBD(
            ownerId = ownerId,
            ownerFileType = payload.ownerType,
            path = existing?.path ?: buildManagedLocalFilePath(
                payload.remoteObjectKey ?: buildCanonicalFileKey(
                    ownerType = payload.ownerType,
                    fileSyncId = payload.syncId,
                    originalName = extractFileName(payload.path),
                )
            ),
            remoteObjectKey = payload.remoteObjectKey,
            remoteStorageProvider = payload.remoteStorageProvider,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.fileDao.upsertFiles(listOf(incoming))
    }

    private suspend fun requireVendor(syncId: String): Vendor {
        return db.vendorDao.getVendorBySyncId(syncId)
            ?: error("Missing vendor dependency for syncId=$syncId")
    }

    private suspend fun requireProduct(syncId: String): Product {
        return db.productDao.getProductBySyncId(syncId)
            ?: error("Missing product dependency for syncId=$syncId")
    }

    private suspend fun requireDeclaration(syncId: String): Declaration {
        return db.declarationDao.getDeclarationBySyncId(syncId)
            ?: error("Missing declaration dependency for syncId=$syncId")
    }

    private suspend fun requireBatch(syncId: String): BatchBD {
        return db.batchDao.getBatchBySyncId(syncId)
            ?: error("Missing batch dependency for syncId=$syncId")
    }

    private suspend fun requireTransaction(syncId: String): Transact {
        return db.transactionDao.getTransactionBySyncId(syncId)
            ?: error("Missing transaction dependency for syncId=$syncId")
    }

    private suspend fun requireMovement(syncId: String): BatchMovement {
        return db.batchMovementDao.getMovementBySyncId(syncId)
            ?: error("Missing movement dependency for syncId=$syncId")
    }

    private suspend fun requireOwnerId(
        ownerType: OwnerType,
        ownerSyncId: String,
    ): Int {
        return when (ownerType) {
            OwnerType.DECLARATION -> requireDeclaration(ownerSyncId).id
            OwnerType.PRODUCT -> requireProduct(ownerSyncId).id
            OwnerType.VENDOR -> requireVendor(ownerSyncId).id
            OwnerType.DOCUMENT -> db.documentDao.getDocumentBySyncId(ownerSyncId)?.id
                ?: error("Missing document dependency for syncId=$ownerSyncId")
            OwnerType.TRANSACTION -> requireTransaction(ownerSyncId).id
            OwnerType.EXPENSE -> db.expenseDao.getExpenseBySyncId(ownerSyncId)?.id
                ?: error("Missing expense dependency for syncId=$ownerSyncId")
        }
    }

    private inline fun <reified T> RemotePullChange.decodePayload(): T {
        val rawPayload = payloadJson ?: error("Missing payload for change `${entityTable}:${entitySyncId}`")
        return runCatching {
            val envelope = json.decodeFromString<SyncPayloadEnvelope<T>>(rawPayload)
            require(envelope.version in 1..CURRENT_SYNC_PAYLOAD_VERSION) {
                "Unsupported payload version `${envelope.version}` for `${entityTable}:${entitySyncId}`"
            }
            envelope.payload
        }.getOrElse {
            json.decodeFromString(rawPayload)
        }
    }
}

private fun entityPriority(entityTable: String): Int {
    return when (entityTable) {
        VENDOR_TABLE_NAME -> 0
        DOCUMENT_TABLE_NAME -> 1
        PRODUCT_TABLE_NAME -> 2
        SAFETY_STOCK_TABLE_NAME -> 3
        TRANSACTION_TABLE_NAME -> 4
        DECLARATIONS_TABLE_NAME -> 5
        BATCH_TABLE_NAME -> 6
        PRODUCT_DECLARATION_TABLE_NAME -> 7
        COMPOSITION_TABLE_NAME -> 8
        BATCH_MOVEMENT_TABLE_NAME -> 9
        REMINDER_TABLE_NAME -> 10
        EXPENSE_TABLE_NAME -> 11
        BUY_TABLE_NAME -> 12
        SALE_TABLE_NAME -> 13
        FILE_TABLE_NAME -> 14
        else -> 100
    }
}

private fun isStale(existingUpdatedAt: LocalDateTime, incomingUpdatedAt: LocalDateTime): Boolean {
    return existingUpdatedAt.toString() >= incomingUpdatedAt.toString()
}

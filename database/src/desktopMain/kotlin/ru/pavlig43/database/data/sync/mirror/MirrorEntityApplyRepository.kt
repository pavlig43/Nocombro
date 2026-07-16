package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDateTime
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
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_TABLE_NAME
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.buildCanonicalFileKey
import ru.pavlig43.database.data.files.buildManagedLocalFilePath
import ru.pavlig43.database.data.files.extractFileName
import ru.pavlig43.database.data.product.COMPOSITION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_DECLARATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_SPECIFICATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductSpecification
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

private typealias MirrorEntityApplyChange = MirrorPushEntityChange

private enum class MirrorApplyChangeType {
    UPSERT,
    DELETE,
}

private val MirrorEntityApplyChange.entitySyncId: String
    get() = row.syncId

private val MirrorEntityApplyChange.changedAt: LocalDateTime
    get() = row.versionAt()

private val MirrorEntityApplyChange.changeType: MirrorApplyChangeType
    get() = if (row.deletedAt == null) MirrorApplyChangeType.UPSERT else MirrorApplyChangeType.DELETE

/**
 * Применяет typed mirror rows к конкретным Room DAO и восстанавливает локальные FK.
 *
 * Для каждой таблицы repository:
 * - находит существующую строку по стабильному `sync_id`;
 * - отбрасывает incoming-версию, если локальная новее;
 * - разрешает родительские `*_sync_id` в локальные числовые id;
 * - выполняет upsert либо локальное удаление/tombstone согласно модели таблицы.
 *
 * [MirrorLocalApplyRepository] отвечает за общий порядок таблиц и journal, тогда
 * как этот класс инкапсулирует table-specific mapping.
 */
@Suppress("TooManyFunctions")
class MirrorEntityApplyRepository(
    private val db: NocombroDatabase,
) {

    /**
     * Атомарно применяет набор изменений в порядке parent-before-child.
     *
     * Для delete вызывающий слой обычно передает уже обратный FK-порядок; внутренняя
     * сортировка прежде всего обеспечивает детерминированность внутри одного вызова.
     */
    suspend fun applyChanges(
        changes: List<MirrorPushEntityChange>,
    ) {
        if (changes.isEmpty()) {
            return
        }

        db.inTransaction {
            val sortedChanges = changes
                .sortedWith(compareBy<MirrorPushEntityChange>({ it.table.applyOrder }, { it.row.syncId }))
            for (change in sortedChanges) {
                applySingleChange(change)
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun applySingleChange(change: MirrorPushEntityChange) {
        when (change.table.tableName) {
            VENDOR_TABLE_NAME -> applyVendor(change)
            DOCUMENT_TABLE_NAME -> applyDocument(change)
            PRODUCT_TABLE_NAME -> applyProduct(change)
            PRODUCT_SPECIFICATION_TABLE_NAME -> applyProductSpecification(change)
            SAFETY_STOCK_TABLE_NAME -> applySafetyStock(change)
            TRANSACTION_TABLE_NAME -> applyTransaction(change)
            DECLARATIONS_TABLE_NAME -> applyDeclaration(change)
            BATCH_TABLE_NAME -> applyBatch(change)
            PRODUCT_DECLARATION_TABLE_NAME -> applyProductDeclaration(change)
            COMPOSITION_TABLE_NAME -> applyComposition(change)
            BATCH_MOVEMENT_TABLE_NAME -> applyBatchMovement(change)
            REMINDER_TABLE_NAME -> applyReminder(change)
            EXPENSE_TABLE_NAME -> applyExpense(change)
            EXPERIMENT_TABLE_NAME -> applyExperiment(change)
            EXPERIMENT_ENTRY_TABLE_NAME -> applyExperimentEntry(change)
            EXPERIMENT_REMINDER_TABLE_NAME -> applyExperimentReminder(change)
            BUY_TABLE_NAME -> applyBuy(change)
            SALE_TABLE_NAME -> applySale(change)
            FILE_TABLE_NAME -> applyFile(change)
        }
    }

    private suspend fun applyVendor(change: MirrorEntityApplyChange) {
        val existing = db.vendorDao.getVendorBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.vendorDao.updateVendor(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as VendorMirrorRow
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

    private suspend fun applyDocument(change: MirrorEntityApplyChange) {
        val existing = db.documentDao.getDocumentBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.documentDao.updateDocument(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as DocumentMirrorRow
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

    private suspend fun applyProduct(change: MirrorEntityApplyChange) {
        val existing = db.productDao.getProductBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.productDao.updateProduct(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as ProductMirrorRow
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

    private suspend fun applySafetyStock(change: MirrorEntityApplyChange) {
        val existing = db.safetyStockDao.getBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.safetyStockDao.upsert(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as SafetyStockMirrorRow
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

    private suspend fun applyProductSpecification(change: MirrorEntityApplyChange) {
        val existing = db.productSpecificationDao.getBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.productSpecificationDao.upsert(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as ProductSpecificationMirrorRow
        val product = requireProduct(payload.productSyncId)
        val incoming = ProductSpecification(
            productId = product.id,
            dosage = payload.dosage,
            composition = payload.composition,
            shelfLifeText = payload.shelfLifeText,
            storageConditions = payload.storageConditions,
            appearance = payload.appearance,
            color = payload.color,
            smell = payload.smell,
            taste = payload.taste,
            physicalChemicalIndicators = payload.physicalChemicalIndicators,
            microbiologicalIndicators = payload.microbiologicalIndicators,
            toxicElements = payload.toxicElements,
            allergens = payload.allergens,
            gmoInfo = payload.gmoInfo,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.productSpecificationDao.upsert(incoming)
    }

    private suspend fun applyTransaction(change: MirrorEntityApplyChange) {
        val existing = db.transactionDao.getTransactionBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.transactionDao.updateTransaction(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as TransactionMirrorRow
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

    private suspend fun applyDeclaration(change: MirrorEntityApplyChange) {
        val existing = db.declarationDao.getDeclarationBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.declarationDao.updateDeclaration(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as DeclarationMirrorRow
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

    private suspend fun applyBatch(change: MirrorEntityApplyChange) {
        val existing = db.batchDao.getBatchBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.batchDao.updateBatch(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as BatchMirrorRow
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

    private suspend fun applyProductDeclaration(change: MirrorEntityApplyChange) {
        val existing = db.productDeclarationDao.getProductDeclarationBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.productDeclarationDao.upsertProductDeclarations(
                        listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                    )
                }
            }
            return
        }

        val payload = change.row as ProductDeclarationMirrorRow
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

    private suspend fun applyComposition(change: MirrorEntityApplyChange) {
        val existing = db.compositionDao.getCompositionBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.compositionDao.upsertComposition(listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt)))
                }
            }
            return
        }

        val payload = change.row as CompositionMirrorRow
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

    private suspend fun applyBatchMovement(change: MirrorEntityApplyChange) {
        val existing = db.batchMovementDao.getMovementBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.batchMovementDao.upsertMovement(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as BatchMovementMirrorRow
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

    private suspend fun applyReminder(change: MirrorEntityApplyChange) {
        val existing = db.reminderDao.getReminderBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.reminderDao.upsertAll(listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt)))
                }
            }
            return
        }

        val payload = change.row as ReminderMirrorRow
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

    private suspend fun applyExpense(change: MirrorEntityApplyChange) {
        val existing = db.expenseDao.getExpenseBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.expenseDao.updateExpense(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as ExpenseMirrorRow
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

    private suspend fun applyExperiment(change: MirrorEntityApplyChange) {
        val existing = db.experimentDao.getExperimentBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.experimentDao.upsert(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as ExperimentMirrorRow
        val incoming = Experiment(
            title = payload.title,
            ideaDescription = payload.ideaDescription,
            isArchived = payload.isArchived,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.experimentDao.upsert(incoming)
    }

    private suspend fun applyExperimentEntry(change: MirrorEntityApplyChange) {
        val existing = db.experimentEntryDao.getEntryBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.experimentEntryDao.upsert(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as ExperimentEntryMirrorRow
        val experiment = requireExperiment(payload.experimentSyncId)
        val incoming = ExperimentEntry(
            experimentId = experiment.id,
            entryDate = payload.entryDate,
            createdAt = payload.createdAt,
            content = payload.content,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.experimentEntryDao.upsert(incoming)
    }

    private suspend fun applyExperimentReminder(change: MirrorEntityApplyChange) {
        val existing = db.experimentReminderDao.getReminderBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.experimentReminderDao.upsert(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as ExperimentReminderMirrorRow
        val experiment = requireExperiment(payload.experimentSyncId)
        val incoming = ExperimentReminder(
            experimentId = experiment.id,
            text = payload.text,
            reminderDateTime = payload.reminderDateTime,
            id = existing?.id ?: 0,
            syncId = payload.syncId,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        if (existing != null && isStale(existing.updatedAt, incoming.updatedAt)) return
        db.experimentReminderDao.upsert(incoming)
    }

    private suspend fun applyBuy(change: MirrorEntityApplyChange) {
        val existing = db.buyDao.getBuyBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.buyDao.upsertBuyBd(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as BuyMirrorRow
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

    private suspend fun applySale(change: MirrorEntityApplyChange) {
        val existing = db.saleDao.getSaleBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.saleDao.upsertSaleBd(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                }
            }
            return
        }

        val payload = change.row as SaleMirrorRow
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

    private suspend fun applyFile(change: MirrorEntityApplyChange) {
        val existing = db.fileDao.getFileBySyncId(change.entitySyncId)
        if (change.changeType == MirrorApplyChangeType.DELETE) {
            existing?.let {
                if (!isStale(it.updatedAt, change.changedAt)) {
                    db.fileDao.upsertFiles(
                        listOf(it.copy(deletedAt = change.changedAt, updatedAt = change.changedAt))
                    )
                }
            }
            return
        }

        val payload = change.row as FileMirrorRow
        val ownerId = requireOwnerId(
            ownerType = payload.ownerType,
            ownerSyncId = payload.ownerSyncId,
        )
        val incoming = FileBD(
            ownerId = ownerId,
            ownerFileType = payload.ownerType,
            displayName = payload.displayName,
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
            OwnerType.EXPERIMENT_ENTRY -> db.experimentEntryDao.getEntryBySyncId(ownerSyncId)?.id
                ?: error("Missing experiment entry dependency for syncId=$ownerSyncId")
        }
    }

    private suspend fun requireExperiment(syncId: String): Experiment {
        return db.experimentDao.getExperimentBySyncId(syncId)
            ?: error("Missing experiment dependency for syncId=$syncId")
    }

}

private fun isStale(existingUpdatedAt: LocalDateTime, incomingUpdatedAt: LocalDateTime): Boolean {
    return existingUpdatedAt.toString() >= incomingUpdatedAt.toString()
}

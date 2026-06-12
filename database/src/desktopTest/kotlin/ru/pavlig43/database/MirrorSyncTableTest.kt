package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.sync.mirror.BatchCostPriceMirrorMapper
import ru.pavlig43.database.data.sync.mirror.BatchCostPriceMirrorRow
import ru.pavlig43.database.data.sync.mirror.BatchMirrorRow
import ru.pavlig43.database.data.sync.mirror.BatchMovementMirrorRow
import ru.pavlig43.database.data.sync.mirror.BuyMirrorRow
import ru.pavlig43.database.data.sync.mirror.CompositionMirrorRow
import ru.pavlig43.database.data.sync.mirror.DocumentMirrorRow
import ru.pavlig43.database.data.sync.mirror.DeclarationMirrorRow
import ru.pavlig43.database.data.sync.mirror.ExperimentEntryMirrorRow
import ru.pavlig43.database.data.sync.mirror.ExperimentMirrorRow
import ru.pavlig43.database.data.sync.mirror.ExperimentReminderMirrorRow
import ru.pavlig43.database.data.sync.mirror.ExpenseMirrorRow
import ru.pavlig43.database.data.sync.mirror.FileMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.ProductMirrorRow
import ru.pavlig43.database.data.sync.mirror.ProductDeclarationMirrorRow
import ru.pavlig43.database.data.sync.mirror.ProductSpecificationMirrorRow
import ru.pavlig43.database.data.sync.mirror.ReminderMirrorRow
import ru.pavlig43.database.data.sync.mirror.SafetyStockMirrorRow
import ru.pavlig43.database.data.sync.mirror.SaleMirrorRow
import ru.pavlig43.database.data.sync.mirror.TransactionMirrorRow
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase

class MirrorSyncTableTest : DesktopMainDispatcherFunSpec({

    test("mirrored business tables match approved v1 inventory") {
        MirrorSyncTable.mirroredBusinessTables.map(MirrorSyncTable::tableName) shouldBe
            listOf(
                "vendor",
                "document",
                "declaration",
                "product",
                "transact",
                "experiment",
                "product_specification",
                "safety_stock",
                "experiment_entry",
                "experiment_reminder",
                "product_declaration",
                "composition",
                "batch",
                "batch_cost_price",
                "batch_movement",
                "reminder",
                "expense",
                "buy",
                "sale",
                "file",
            )
    }

    test("batch cost price is mirrored after its batch") {
        MirrorSyncTable.fromTableName("batch_cost_price") shouldBe MirrorSyncTable.BATCH_COST_PRICE
        MirrorSyncTable.BATCH_COST_PRICE.applyOrder shouldBe MirrorSyncTable.BATCH.applyOrder + 1
    }

    test("batch cost price maps through batch sync id") {
        withSeededTestDatabase { db ->
            val batch = db.batchDao.getBatch(1)
            val entity = BatchCostPriceEntity(
                batchId = batch.id,
                costPricePerUnit = 12_345,
                batchSyncId = batch.syncId,
                updatedAt = LocalDateTime(2026, 6, 9, 10, 30),
            )
            val mapper = BatchCostPriceMirrorMapper(db)

            val row = mapper.toMirrorRow(entity)
            row.batchSyncId shouldBe batch.syncId
            mapper.fromMirrorRow(row) shouldBe entity
        }
    }

    test("independent databases create the same batch cost mirror row id") {
        val first = createSeededManagedTestDatabase()
        val second = createSeededManagedTestDatabase()
        try {
            suspend fun createRow(db: NocombroDatabase): BatchCostPriceMirrorRow {
                val batchId = db.batchDao.createBatch(
                    BatchBD(
                        id = 0,
                        productId = 1,
                        dateBorn = LocalDate(2026, 6, 11),
                        declarationId = 1,
                        syncId = "shared-batch-sync-id",
                    )
                ).toInt()
                val entity = BatchCostPriceEntity(
                    batchId = batchId,
                    costPricePerUnit = 54_321,
                    batchSyncId = "shared-batch-sync-id",
                )
                db.batchCostDao.upsert(listOf(entity))
                return BatchCostPriceMirrorMapper(db).toMirrorRow(entity)
            }

            createRow(first.database).syncId shouldBe "shared-batch-sync-id"
            createRow(second.database).syncId shouldBe "shared-batch-sync-id"
        } finally {
            second.close()
            first.close()
        }
    }

    test("local snapshot loads batch cost price rows") {
        withSeededTestDatabase { db ->
            val snapshot = MirrorLocalSnapshotRepository(db).loadSnapshot(
                tables = listOf(MirrorSyncTable.BATCH_COST_PRICE),
            )

            snapshot.rowsByTable.keys shouldBe setOf(MirrorSyncTable.BATCH_COST_PRICE)
            snapshot.rowsByTable
                .getValue(MirrorSyncTable.BATCH_COST_PRICE)
                .all { it is BatchCostPriceMirrorRow } shouldBe true
        }
    }

    test("local snapshot loads first root table group") {
        withSeededTestDatabase { db ->
            val tables = listOf(
                MirrorSyncTable.VENDOR,
                MirrorSyncTable.DOCUMENT,
                MirrorSyncTable.PRODUCT,
                MirrorSyncTable.TRANSACTION,
                MirrorSyncTable.EXPERIMENT,
            )
            val snapshot = MirrorLocalSnapshotRepository(db).loadSnapshot(tables)

            snapshot.rowsByTable.keys shouldBe tables.toSet()
            snapshot.rowsByTable.getValue(MirrorSyncTable.VENDOR).all { it is VendorMirrorRow } shouldBe true
            snapshot.rowsByTable.getValue(MirrorSyncTable.DOCUMENT).all { it is DocumentMirrorRow } shouldBe true
            snapshot.rowsByTable.getValue(MirrorSyncTable.PRODUCT).all { it is ProductMirrorRow } shouldBe true
            snapshot.rowsByTable.getValue(MirrorSyncTable.TRANSACTION).all { it is TransactionMirrorRow } shouldBe true
            snapshot.rowsByTable.getValue(MirrorSyncTable.EXPERIMENT).all { it is ExperimentMirrorRow } shouldBe true
        }
    }

    test("local snapshot resolves parent sync ids for first child group") {
        withSeededTestDatabase { db ->
            val tables = listOf(
                MirrorSyncTable.VENDOR,
                MirrorSyncTable.PRODUCT,
                MirrorSyncTable.EXPERIMENT,
                MirrorSyncTable.DECLARATION,
                MirrorSyncTable.PRODUCT_SPECIFICATION,
                MirrorSyncTable.SAFETY_STOCK,
                MirrorSyncTable.EXPERIMENT_ENTRY,
                MirrorSyncTable.EXPERIMENT_REMINDER,
            )
            val rows = MirrorLocalSnapshotRepository(db).loadSnapshot(tables).rowsByTable
            val vendorIds = rows.getValue(MirrorSyncTable.VENDOR).map { it.syncId }.toSet()
            val productIds = rows.getValue(MirrorSyncTable.PRODUCT).map { it.syncId }.toSet()
            val experimentIds = rows.getValue(MirrorSyncTable.EXPERIMENT).map { it.syncId }.toSet()

            rows.getValue(MirrorSyncTable.DECLARATION)
                .filterIsInstance<DeclarationMirrorRow>()
                .all { it.vendorSyncId in vendorIds } shouldBe true
            rows.getValue(MirrorSyncTable.PRODUCT_SPECIFICATION)
                .filterIsInstance<ProductSpecificationMirrorRow>()
                .all { it.productSyncId in productIds } shouldBe true
            rows.getValue(MirrorSyncTable.SAFETY_STOCK)
                .filterIsInstance<SafetyStockMirrorRow>()
                .all { it.productSyncId in productIds } shouldBe true
            rows.getValue(MirrorSyncTable.EXPERIMENT_ENTRY)
                .filterIsInstance<ExperimentEntryMirrorRow>()
                .all { it.experimentSyncId in experimentIds } shouldBe true
            rows.getValue(MirrorSyncTable.EXPERIMENT_REMINDER)
                .filterIsInstance<ExperimentReminderMirrorRow>()
                .all { it.experimentSyncId in experimentIds } shouldBe true
        }
    }

    test("local snapshot resolves product declaration composition and batch parents") {
        withSeededTestDatabase { db ->
            val tables = listOf(
                MirrorSyncTable.PRODUCT,
                MirrorSyncTable.DECLARATION,
                MirrorSyncTable.PRODUCT_DECLARATION,
                MirrorSyncTable.COMPOSITION,
                MirrorSyncTable.BATCH,
            )
            val rows = MirrorLocalSnapshotRepository(db).loadSnapshot(tables).rowsByTable
            val productIds = rows.getValue(MirrorSyncTable.PRODUCT).map { it.syncId }.toSet()
            val declarationIds = rows.getValue(MirrorSyncTable.DECLARATION).map { it.syncId }.toSet()

            rows.getValue(MirrorSyncTable.PRODUCT_DECLARATION)
                .filterIsInstance<ProductDeclarationMirrorRow>()
                .all {
                    it.productSyncId in productIds &&
                        it.declarationSyncId in declarationIds
                } shouldBe true
            rows.getValue(MirrorSyncTable.COMPOSITION)
                .filterIsInstance<CompositionMirrorRow>()
                .all {
                    it.parentSyncId in productIds &&
                        it.productSyncId in productIds
                } shouldBe true
            rows.getValue(MirrorSyncTable.BATCH)
                .filterIsInstance<BatchMirrorRow>()
                .all {
                    it.productSyncId in productIds &&
                        it.declarationSyncId in declarationIds
                } shouldBe true
        }
    }

    test("local snapshot resolves transaction group parents") {
        withSeededTestDatabase { db ->
            val tables = listOf(
                MirrorSyncTable.VENDOR,
                MirrorSyncTable.TRANSACTION,
                MirrorSyncTable.BATCH,
                MirrorSyncTable.BATCH_MOVEMENT,
                MirrorSyncTable.REMINDER,
                MirrorSyncTable.EXPENSE,
                MirrorSyncTable.BUY,
                MirrorSyncTable.SALE,
            )
            val rows = MirrorLocalSnapshotRepository(db).loadSnapshot(tables).rowsByTable
            val vendorIds = rows.getValue(MirrorSyncTable.VENDOR).map { it.syncId }.toSet()
            val transactionIds = rows.getValue(MirrorSyncTable.TRANSACTION).map { it.syncId }.toSet()
            val batchIds = rows.getValue(MirrorSyncTable.BATCH).map { it.syncId }.toSet()
            val movementIds = rows.getValue(MirrorSyncTable.BATCH_MOVEMENT).map { it.syncId }.toSet()

            rows.getValue(MirrorSyncTable.BATCH_MOVEMENT)
                .filterIsInstance<BatchMovementMirrorRow>()
                .all {
                    it.batchSyncId in batchIds &&
                        it.transactionSyncId in transactionIds
                } shouldBe true
            rows.getValue(MirrorSyncTable.REMINDER)
                .filterIsInstance<ReminderMirrorRow>()
                .all { it.transactionSyncId in transactionIds } shouldBe true
            rows.getValue(MirrorSyncTable.EXPENSE)
                .filterIsInstance<ExpenseMirrorRow>()
                .all { it.transactionSyncId == null || it.transactionSyncId in transactionIds } shouldBe true
            rows.getValue(MirrorSyncTable.BUY)
                .filterIsInstance<BuyMirrorRow>()
                .all {
                    it.transactionSyncId in transactionIds &&
                        it.movementSyncId in movementIds
                } shouldBe true
            rows.getValue(MirrorSyncTable.SALE)
                .filterIsInstance<SaleMirrorRow>()
                .all {
                    it.transactionSyncId in transactionIds &&
                        it.movementSyncId in movementIds &&
                        it.clientSyncId in vendorIds
                } shouldBe true
        }
    }

    test("local snapshot resolves file owner and covers every mirror table") {
        withSeededTestDatabase { db ->
            val product = db.productDao.getProduct(1)
            db.fileDao.upsertFiles(
                listOf(
                    FileBD(
                        ownerId = product.id,
                        ownerFileType = OwnerType.PRODUCT,
                        displayName = "spec.pdf",
                        path = "spec.pdf",
                        syncId = "file-sync-id",
                        updatedAt = LocalDateTime(2026, 6, 10, 10, 0),
                    )
                )
            )

            val snapshot = MirrorLocalSnapshotRepository(db).loadSnapshot(
                MirrorSyncTable.mirroredBusinessTables
            )
            val file = snapshot.rowsByTable
                .getValue(MirrorSyncTable.FILE)
                .single() as FileMirrorRow

            snapshot.rowsByTable.keys shouldBe MirrorSyncTable.mirroredBusinessTables.toSet()
            file.ownerSyncId shouldBe product.syncId
            file.ownerType shouldBe OwnerType.PRODUCT
        }
    }
})

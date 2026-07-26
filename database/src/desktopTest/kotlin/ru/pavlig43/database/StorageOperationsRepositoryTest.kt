package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.storage.StorageOperationsRepository
import ru.pavlig43.database.data.storage.StorageTransferRequest
import ru.pavlig43.database.data.storage.StorageWriteOffRequest
import ru.pavlig43.database.data.transact.StockOperationReason
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase

class StorageOperationsRepositoryTest : DesktopMainDispatcherFunSpec({
    test("partial transfer preserves total stock and write off reduces only selected storage") {
        withSeededTestDatabase { db ->
            val repository = StorageOperationsRepository(db)
            val batchId = 1
            val mainBefore = db.batchMovementDao.getBalance(batchId, StorageLocation.MAIN)
            val experimentalBefore = db.batchMovementDao.getBalance(batchId, StorageLocation.EXPERIMENTAL)
            val expenseCountBefore = db.expenseDao.getAll().size
            val moneyMovementCountBefore = db.moneyDao.getAllMovements().size

            repository.transfer(
                StorageTransferRequest(
                    batchId = batchId,
                    source = StorageLocation.MAIN,
                    target = StorageLocation.EXPERIMENTAL,
                    count = 1_000,
                    occurredAt = LocalDateTime(2026, 3, 20, 12, 0),
                    reason = StockOperationReason.SAMPLE,
                    comment = "test transfer",
                )
            ).getOrThrow()

            val mainAfterTransfer = db.batchMovementDao.getBalance(batchId, StorageLocation.MAIN)
            val experimentalAfterTransfer = db.batchMovementDao.getBalance(batchId, StorageLocation.EXPERIMENTAL)
            mainAfterTransfer shouldBe mainBefore - 1_000
            experimentalAfterTransfer shouldBe experimentalBefore + 1_000
            mainAfterTransfer + experimentalAfterTransfer shouldBe mainBefore + experimentalBefore

            db.transactionDao.getAll().last { it.transactionType == TransactionType.STORAGE_TRANSFER }.apply {
                stockOperationReason shouldBe StockOperationReason.SAMPLE
                comment shouldBe "test transfer"
            }

            repository.writeOff(
                StorageWriteOffRequest(
                    batchId = batchId,
                    source = StorageLocation.EXPERIMENTAL,
                    count = 400,
                    occurredAt = LocalDateTime(2026, 3, 21, 12, 0),
                    reason = StockOperationReason.EXPERIMENT,
                    comment = "test write off",
                )
            ).getOrThrow()

            db.batchMovementDao.getBalance(batchId, StorageLocation.MAIN) shouldBe mainAfterTransfer
            db.batchMovementDao.getBalance(batchId, StorageLocation.EXPERIMENTAL) shouldBe experimentalAfterTransfer - 400
            db.expenseDao.getAll().size shouldBe expenseCountBefore
            db.moneyDao.getAllMovements().size shouldBe moneyMovementCountBefore
        }
    }

    test("return to main is blocked after expiry date") {
        withSeededTestDatabase { db ->
            val repository = StorageOperationsRepository(db)
            db.productDao.updateProduct(
                db.productDao.getProduct(1).copy(shelfLifeDays = 30)
            )

            val batchId = 1
            repository.transfer(
                StorageTransferRequest(
                    batchId = batchId,
                    source = StorageLocation.MAIN,
                    target = StorageLocation.EXPERIMENTAL,
                    count = 1_000,
                    occurredAt = LocalDateTime(2026, 3, 20, 12, 0),
                    reason = StockOperationReason.EXPERIMENT,
                    comment = "",
                )
            ).getOrThrow()
            val expiry = repository.preview(batchId, StorageLocation.EXPERIMENTAL, 100).expiryDate!!
            repository.transfer(
                StorageTransferRequest(
                    batchId = batchId,
                    source = StorageLocation.EXPERIMENTAL,
                    target = StorageLocation.MAIN,
                    count = 100,
                    occurredAt = LocalDateTime(expiry.year, expiry.month, expiry.day, 12, 0),
                    reason = StockOperationReason.RETURN,
                    comment = "",
                )
            ).getOrThrow()

            val afterExpiry = expiry.plus(1, DateTimeUnit.DAY)

            repository.transfer(
                StorageTransferRequest(
                    batchId = batchId,
                    source = StorageLocation.EXPERIMENTAL,
                    target = StorageLocation.MAIN,
                    count = 100,
                    occurredAt = LocalDateTime(afterExpiry.year, afterExpiry.month, afterExpiry.day, 12, 0),
                    reason = StockOperationReason.RETURN,
                    comment = "",
                )
            ).isFailure shouldBe true
        }
    }
})

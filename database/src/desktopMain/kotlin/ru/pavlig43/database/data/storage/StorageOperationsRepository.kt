package ru.pavlig43.database.data.storage

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.transact.StockOperationReason
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.inTransaction

data class StorageOperationPreview(
    val availableCount: Long,
    val costPricePerUnit: Long?,
    val selectedCost: Long,
    val expiryDate: LocalDate?,
)

data class StorageTransferRequest(
    val batchId: Int,
    val source: StorageLocation,
    val target: StorageLocation,
    val count: Long,
    val occurredAt: LocalDateTime,
    val reason: StockOperationReason,
    val comment: String,
)

data class StorageWriteOffRequest(
    val batchId: Int,
    val source: StorageLocation,
    val count: Long,
    val occurredAt: LocalDateTime,
    val reason: StockOperationReason,
    val comment: String,
    val allowMissingCost: Boolean = false,
)

class StorageOperationsRepository(private val db: NocombroDatabase) {
    suspend fun preview(
        batchId: Int,
        source: StorageLocation,
        count: Long,
    ): StorageOperationPreview {
        val batch = db.batchDao.getBatchOut(batchId)
        val cost = batch.costPrice?.costPricePerUnit
        val expiryDate = batch.product.shelfLifeDays
            .takeIf { it > 0 }
            ?.let { batch.batch.dateBorn.plus(it, DateTimeUnit.DAY) }
        return StorageOperationPreview(
            availableCount = db.batchMovementDao.getBalance(batchId, source),
            costPricePerUnit = cost,
            selectedCost = cost?.let { it * count / 1000 } ?: 0,
            expiryDate = expiryDate,
        )
    }

    suspend fun transfer(request: StorageTransferRequest): Result<Unit> = runCatching {
        require(request.source != request.target) { "Исходный и целевой склад совпадают" }
        db.inTransaction {
            validateAvailable(request.batchId, request.source, request.count)
            if (request.target == StorageLocation.MAIN) validateReturnDate(request)
            val transactionId = db.transactionDao.create(
                Transact(
                    transactionType = TransactionType.STORAGE_TRANSFER,
                    createdAt = request.occurredAt,
                    comment = request.comment.trim(),
                    isCompleted = true,
                    stockOperationReason = request.reason,
                )
            ).toInt()
            db.batchMovementDao.createMovement(
                BatchMovement(
                    batchId = request.batchId,
                    movementType = MovementType.OUTGOING,
                    count = request.count,
                    transactionId = transactionId,
                    storageLocation = request.source,
                )
            )
            db.batchMovementDao.createMovement(
                BatchMovement(
                    batchId = request.batchId,
                    movementType = MovementType.INCOMING,
                    count = request.count,
                    transactionId = transactionId,
                    storageLocation = request.target,
                )
            )
        }
    }

    suspend fun writeOff(request: StorageWriteOffRequest): Result<Unit> = runCatching {
        db.inTransaction {
            validateAvailable(request.batchId, request.source, request.count)
            val costPrice = db.batchDao.getBatchOut(request.batchId).costPrice?.costPricePerUnit
            require(costPrice != null || request.allowMissingCost) {
                "Себестоимость не рассчитана. Нужно явно подтвердить списание с нулевой стоимостью"
            }
            val transactionId = db.transactionDao.create(
                Transact(
                    transactionType = TransactionType.WRITE_OFF,
                    createdAt = request.occurredAt,
                    comment = request.comment.trim(),
                    isCompleted = true,
                    stockOperationReason = request.reason,
                )
            ).toInt()
            db.batchMovementDao.createMovement(
                BatchMovement(
                    batchId = request.batchId,
                    movementType = MovementType.OUTGOING,
                    count = request.count,
                    transactionId = transactionId,
                    storageLocation = request.source,
                    updatedAt = defaultUpdatedAt(),
                )
            )
        }
    }

    private suspend fun validateAvailable(batchId: Int, source: StorageLocation, count: Long) {
        require(count > 0) { "Количество должно быть больше нуля" }
        val balance = db.batchMovementDao.getBalance(batchId, source)
        require(count <= balance) { "Количество больше остатка на складе" }
    }

    private suspend fun validateReturnDate(request: StorageTransferRequest) {
        val batch = db.batchDao.getBatchOut(request.batchId)
        val shelfLifeDays = batch.product.shelfLifeDays
        if (shelfLifeDays <= 0) return
        val expiryDate = batch.batch.dateBorn.plus(shelfLifeDays, DateTimeUnit.DAY)
        require(request.occurredAt.date <= expiryDate) {
            "После окончания срока годности вернуть партию на основной склад нельзя"
        }
    }
}

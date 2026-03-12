package ru.pavlig43.database.data.transact.pf.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.BatchOut
import ru.pavlig43.database.data.transact.pf.PfBD

/**
 * DAO для работы с полуфабрикатом (PF) в ОПЗС транзакциях.
 */
@Dao
abstract class PfDao {

    /**
     * Получает внутреннюю структуру с @Relation для полуфабриката.
     */
    @Transaction
    @Query("""
        SELECT * FROM batch_movement
        WHERE transaction_id = :transactionId
        AND movement_type = 'INCOMING'
        LIMIT 1
    """)
    internal abstract suspend fun getInternalPf(transactionId: Int): InternalPf?

    /**
     * Получает полуфабрикат для указанной транзакции.
     *
     * @param transactionId Идентификатор транзакции
     * @return DTO полуфабриката или null, если не найден
     */
    suspend fun getPf(transactionId: Int): PfBD? {
        return getInternalPf(transactionId)?.toPfBD()
    }
}

/**
 * Внутренний DTO с @Relation для загрузки данных из Room.
 */
internal data class InternalPf(
    @Embedded
    val movement: BatchMovement,

    @Relation(
        entity = BatchBD::class,
        parentColumn = "batch_id",
        entityColumn = "id"
    )
    val batchOut: BatchOut
)

/**
 * Конвертирует InternalPf в PfBD.
 */
private fun InternalPf.toPfBD(): PfBD {
    return PfBD(
        transactionId = movement.transactionId,
        batchId = movement.batchId,
        movementId = movement.id,
        count = movement.count,
        productId = batchOut.product.id,
        productName = batchOut.product.displayName,
        declarationId = batchOut.declaration.id,
        declarationName = batchOut.declaration.displayName,
        dateBorn = batchOut.batch.dateBorn,
        vendorName = batchOut.declaration.vendorName,
        id = movement.transactionId
    )
}

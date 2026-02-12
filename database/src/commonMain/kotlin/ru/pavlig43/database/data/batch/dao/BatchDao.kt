package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.batch.BatchBD

@Dao
interface BatchDao {

    @Insert
    suspend fun createBatch(batch: BatchBD): Long

    @Query("""
        SELECT id FROM batch
        WHERE product_id = :productId
        AND declaration_id = :declarationId
        AND date_born = :dateBorn
        LIMIT 1
    """)
    suspend fun getBatchIdByProductDeclarationAndDate(
        productId: Int,
        declarationId: Int,
        dateBorn: LocalDate
    ): Int?

    @Query("""
        SELECT DISTINCT batch_id
        FROM batch_movement
        WHERE transaction_id = :transactionId
    """)
    suspend fun getBatchIdsByTransactionId(transactionId: Int): List<Int>
}

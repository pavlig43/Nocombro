package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.batch.BatchBD

@Dao
interface BatchDao {



    @Update
    suspend fun updateBatch(batch: BatchBD)

    @Insert
    suspend fun createBatch(batchBD: BatchBD): Long

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

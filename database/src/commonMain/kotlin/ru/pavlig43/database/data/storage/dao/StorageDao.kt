package ru.pavlig43.database.data.storage.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.core.mapParallel
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.storage.StorageBatch
import ru.pavlig43.database.data.storage.StorageProduct

@Dao
abstract class StorageDao {
    @Transaction
    @Query("SELECT * FROM batch_movement ")
    internal abstract fun observeOnAllMovements(): Flow<List<MovementOut>>


    fun observeOnStorageBatches(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<String>> {
        val storageBatches: Flow<List<StorageProduct>> = observeOnAllMovements().map { fillList ->
            val filteredList = fillList.filter { it.transaction.createdAt <= end }
            filteredList.groupBy { it.batchOut.product }

                .run {
                    this.values.mapParallel(Dispatchers.IO) { movementOuts: List<MovementOut> ->
                        val product = movementOuts.first().batchOut.product
                        val productId = product.id
                        val productName = product.displayName

                        val batches = movementOuts
                            .groupBy { it.batchOut.batch }
                            .map { (batch, moves) ->
                                val batchId = batch.id
                                val batchName = "($batchId) ${batch.dateBorn.format(dateFormat)}"
                                var balanceBeforeStart = 0
                                var incoming = 0
                                var outgoing = 0

                                moves.forEach { move ->
                                    val count = move.movement.count
                                    val type = move.movement.movementType
                                    val transactionDt = move.transaction.createdAt
                                    val sign = when(type) {
                                        MovementType.INCOMING -> 1
                                        MovementType.OUTGOING -> -1
                                    }

                                    when {
                                        transactionDt < start -> balanceBeforeStart += count * sign
                                        else -> {
                                            if (type == MovementType.INCOMING) incoming += count
                                            else outgoing += count
                                        }
                                    }
                                }

                                StorageBatch(
                                    batchId = batchId,
                                    batchName = batchName,
                                    balanceBeforeStart = balanceBeforeStart,
                                    incoming = incoming,
                                    outgoing = outgoing,
                                    balanceOnEnd = balanceBeforeStart + incoming - outgoing
                                )
                            }

                        StorageProduct(
                            productId = productId,
                            productName = productName,
                            batches = batches
                        )
                    }
                }
        }
        return storageBatches.mapValues { "$it \n" }
    }
}
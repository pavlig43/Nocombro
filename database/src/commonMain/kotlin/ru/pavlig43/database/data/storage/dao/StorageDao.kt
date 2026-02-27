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

    @Transaction
    @Query(
        """
        SELECT bm.* FROM batch_movement bm
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE t.created_at <= :end
    """
    )
    internal abstract fun observeMovementsUntil(end: LocalDateTime): Flow<List<MovementOut>>


    fun observeOnStorageBatches(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<StorageProduct>> {
        return observeMovementsUntil(end).map { fillList ->
            fillList.groupBy { it.batchOut.product }
                .run {
                    this.values.mapParallel(Dispatchers.Default) { movementOuts: List<MovementOut> ->
                        val product = movementOuts.first().batchOut.product
                        val productId = product.id
                        val productName = product.displayName

                        val batches = movementOuts
                            .groupBy { it.batchOut.batch }
                            .map { (batch, moves) ->
                                val batchId = batch.id
                                val batchName = "($batchId) ${batch.dateBorn.format(dateFormat)}"

                                val (balanceBeforeStart, incoming, outgoing) = moves.fold(
                                    Triple(
                                        0,
                                        0,
                                        0
                                    )
                                ) { (accBefore, accIn, accOut), move ->
                                    val count = move.movement.count
                                    val type = move.movement.movementType
                                    val dt = move.transaction.createdAt
                                    when {
                                        dt < start && type == MovementType.INCOMING -> Triple(
                                            accBefore + count,
                                            accIn,
                                            accOut
                                        )

                                        dt < start && type == MovementType.OUTGOING -> Triple(
                                            accBefore - count,
                                            accIn,
                                            accOut
                                        )

                                        type == MovementType.INCOMING -> Triple(accBefore, accIn + count, accOut)
                                        type == MovementType.OUTGOING -> Triple(accBefore, accIn, accOut + count)
                                        else -> Triple(accBefore, accIn, accOut)
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

                        val totals = batches.fold(Triple(0, 0, 0)) { (accBefore, accIn, accOut), batch ->
                            Triple(
                                accBefore + batch.balanceBeforeStart,
                                accIn + batch.incoming,
                                accOut + batch.outgoing
                            )
                        }

                        StorageProduct(
                            productId = productId,
                            productName = productName,
                            balanceBeforeStart = totals.first,
                            incoming = totals.second,
                            outgoing = totals.third,
                            balanceOnEnd = totals.first + totals.second - totals.third,
                            batches = batches
                        )
                    }
                }
        }
    }
}
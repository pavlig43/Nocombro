package ru.pavlig43.database.data.storage.dao

// Storage DAO для работы со складом
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
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.storage.BatchMovementWithBalanceBD
import ru.pavlig43.database.data.storage.BatchMovementWithBalanceInfoBD
import ru.pavlig43.database.data.storage.StorageBatch
import ru.pavlig43.database.data.storage.StorageProduct

@Dao
abstract class StorageDao {

    /**
     * Наблюдает за всеми движениями партий до указанной даты (включительно).
     */
    @Transaction
    @Query(
        """
        SELECT bm.* FROM batch_movement bm
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE t.created_at <= :end
    """
    )
    internal abstract fun observeMovementsUntil(end: LocalDateTime): Flow<List<MovementOut>>

    /**
     * Наблюдает за остатками на складе с разбивкой по продуктам и партиям.
     *
     * Для каждого продукта рассчитывает:
     * - Начальный баланс (сумма по всем партиям продукта)
     * - Поступления в периоде start..end
     * - Отпуски в периоде start..end
     * - Конечный баланс
     *
     * Для каждой партии продукта рассчитывает те же показатели.
     *
     * Архитектура:
     * 1. SQL получает все движения
     * 2. Kotlin фильтрует по дате и рассчитывает баланс
     * 3. Показывает партии с начальным балансом даже без движений в периоде
     *
     * @param start Начало периода (включительно для движений, не включительно для начального баланса)
     * @param end Конец периода (включительно)
     * @return Flow со списком продуктов, отсортированных по названию
     */
    fun observeOnStorageProduct(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<StorageProduct>> {
        return observeMovementsUntil(end).map { fillList ->
            fillList
                .groupBy { it.batchOut.product }
                .values
                .mapParallel(Dispatchers.Default) { movementOuts ->
                    val product = movementOuts.first().batchOut.product
                    val productId = product.id
                    val productName = product.displayName

                    val batches = movementOuts
                        .groupBy { it.batchOut.batch }
                        .entries
                        .sortedBy { it.key.dateBorn }
                        .map { (batch, moves) ->
                            val batchId = batch.id
                            val batchName = "($batchId) ${batch.dateBorn.format(dateFormat)}"

//                            // Сначала считаем полный баланс до каждой даты
//                            val balanceBeforePeriod = moves
//                                .filter { it.transaction.createdAt < start }
//                                .fold(0) { acc, move ->
//                                    val count = move.movement.count
//                                    when (move.movement.movementType) {
//                                        MovementType.INCOMING -> acc + count
//                                        MovementType.OUTGOING -> acc - count
//                                    }
//                                }
                            val (balanceBeforePeriod, incoming, outgoing) = moves.fold(
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
                                    else -> throw IllegalArgumentException("Недопустимая ветка")
                                }
                            }

                            StorageBatch(
                                batchId = batchId,
                                batchName = batchName,
                                balanceBeforeStart = balanceBeforePeriod,
                                incoming = incoming,
                                outgoing = outgoing,
                                balanceOnEnd = balanceBeforePeriod + incoming - outgoing
                            )
                        }

                    val totals = batches.fold(Triple(0, 0, 0)) { acc, batch ->
                        Triple(
                            acc.first + batch.balanceBeforeStart,
                            acc.second + batch.incoming,
                            acc.third + batch.outgoing
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
                .sortedBy { it.productName }
        }
    }

    /**
     * Наблюдает за всеми движениями партии до указанной даты (включительно).
     */
    @Transaction
    @Query(
        """
        SELECT bm.* FROM batch_movement bm
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE bm.batch_id = :batchId
        AND t.created_at <= :end
        ORDER BY t.created_at
        """
    )
    internal abstract fun observeMovementsByBatchIdUntil(
        batchId: Int,
        end: LocalDateTime
    ): Flow<List<MovementOut>>

    /**
     * Наблюдает за движениями партии с накопительным балансом.
     *
     * Для каждого движения рассчитывает:
     * - balanceBeforeStart: баланс ДО этого движения
     * - incoming/outgoing: количество в этом движении
     * - balanceOnEnd: баланс ПОСЛЕ этого движения
     *
     * Архитектура:
     * 1. SQL получает все движения партии, отсортированные по дате
     * 2. Kotlin фильтрует по дате и рассчитывает накопительный баланс
     * 3. Показывает начальный баланс даже без движений в периоде
     *
     * @param batchId ID партии
     * @param start Начало периода (включительно для движений, не включительно для начального баланса)
     * @param end Конец периода (включительно) - не используется в SQL, только для понимания контекста
     * @return Flow с информацией о партии и списках движений с балансами
     */
    fun observeBatchMovementsWithBalance(
        batchId: Int,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<BatchMovementWithBalanceInfoBD> {
        return observeMovementsByBatchIdUntil(batchId, end).map { allMovements ->
            val productName = allMovements.firstOrNull()?.batchOut?.product?.displayName ?: ""

            // Считаем начальный баланс до start (не включая start)
            val balanceBeforeStart = allMovements
                .filter { it.transaction.createdAt < start }
                .fold(0) { acc, move ->
                    val count = move.movement.count
                    when (move.movement.movementType) {
                        MovementType.INCOMING -> acc + count
                        MovementType.OUTGOING -> acc - count
                    }
                }

            // Фильтруем движения в периоде (включая start) и считаем накопительный баланс
            val movementsWithBalance = allMovements
                .filter { it.transaction.createdAt >= start }
                .fold(mutableListOf<Pair<Int, BatchMovementWithBalanceBD>>()) { acc, movementOut ->
                    val prevBalance = acc.lastOrNull()?.first ?: balanceBeforeStart
                    val dt = movementOut.transaction.createdAt
                    val count = movementOut.movement.count
                    val isIncoming = movementOut.movement.movementType == MovementType.INCOMING
                    val incoming = if (isIncoming) count else 0
                    val outgoing = if (!isIncoming) count else 0
                    val balanceOnEnd = prevBalance + incoming - outgoing

                    acc.add(
                        balanceOnEnd to BatchMovementWithBalanceBD(
                            movementDate = dt,
                            balanceBeforeStart = prevBalance,
                            incoming = incoming,
                            outgoing = outgoing,
                            balanceOnEnd = balanceOnEnd,
                            transactionId = movementOut.movement.transactionId
                        )
                    )
                    acc
                }

            // Если нет движений в периоде, но есть начальный баланс - добавляем одну строку
            val result = if (movementsWithBalance.isEmpty() && balanceBeforeStart != 0) {
                listOf(
                    BatchMovementWithBalanceBD(
                        movementDate = start,
                        balanceBeforeStart = balanceBeforeStart,
                        incoming = 0,
                        outgoing = 0,
                        balanceOnEnd = balanceBeforeStart,
                        transactionId = allMovements.first().movement.transactionId
                    )
                )
            } else {
                movementsWithBalance.map { it.second }
            }

            BatchMovementWithBalanceInfoBD(
                batchId = batchId,
                productName = productName,
                movements = result
            )
        }
    }
}


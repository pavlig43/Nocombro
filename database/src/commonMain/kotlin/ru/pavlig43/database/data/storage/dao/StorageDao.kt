package ru.pavlig43.database.data.storage.dao

// Storage DAO для работы со складом
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

private data class MovementBalanceCalculation(
    val balanceBefore: Int,
    val incoming: Int,
    val outgoing: Int
) {
    companion object {
        val ZERO = MovementBalanceCalculation(0, 0, 0)
    }
}

/**
 * Результат запроса начального баланса партии.
 * Используется в Room для маппинга агрегатных запросов.
 */
internal data class BatchBalanceBD(
    @ColumnInfo("batch_id")
    val batchId: Int,
    val balance: Int
)

/**
 * Результат запроса начального баланса партии.
 * Используется для одиночной партии.
 */
internal data class SingleBatchBalanceBD(
    val balance: Int
)

@Dao
abstract class StorageDao {

    /**
     * Наблюдает за движениями партий в указанном временном диапазоне.
     */
    @Transaction
    @Query(
        """
        SELECT bm.* FROM batch_movement bm
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE t.created_at >= :start
        AND t.created_at <= :end
    """
    )
    internal abstract fun observeMovementsInRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<MovementOut>>

    /**
     * Наблюдает за начальным балансом всех партий до указанной даты.
     *
     * Для каждой партии рассчитывает баланс на основе всех движений до [start]:
     * - INCOMING добавляется к балансу
     * - OUTGOING вычитается из баланса
     *
     * @param start Дата, до которой учитываются движения (не включительно)
     * @return Flow со списком пар [batchId, баланс]
     */
    @Transaction
    @Query(
        """
        SELECT
        bm.batch_id,
        COALESCE(SUM(CASE WHEN bm.movement_type = 'INCOMING' THEN bm.count ELSE -bm.count END), 0) as balance
        FROM batch_movement bm
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE t.created_at < :start
        GROUP BY bm.batch_id
    """
    )
    internal abstract fun observeInitialBalanceForAllBatches(
        start: LocalDateTime
    ): Flow<List<BatchBalanceBD>>

    /**
     * Конвертирует список [BatchBalanceBD] в Map [batchId -> баланс].
     */
    private fun Flow<List<BatchBalanceBD>>.toBatchIdMap(): Flow<Map<Int, Int>> {
        return map { list -> list.associate { it.batchId to it.balance } }
    }

    /**
     * Конвертирует список [SingleBatchBalanceBD] в Int.
     * Если список пуст, возвращает 0.
     */
    private fun Flow<List<SingleBatchBalanceBD>>.toInt(): Flow<Int> {
        return map { list -> list.firstOrNull()?.balance ?: 0 }
    }

    /**
     * Рассчитывает incoming/outgoing для списка движений.
     *
     * Применяется к списку движений, который уже отфильтрован по временному диапазону.
     * Начальный баланс не рассчитывается — он берётся из [observeInitialBalanceForAllBatches].
     *
     * @return [MovementBalanceCalculation] с рассчитанными incoming/outgoing (balanceBefore = 0)
     */
    private fun List<MovementOut>.calculateMovementBalance(): MovementBalanceCalculation {
        return fold(MovementBalanceCalculation.ZERO) { acc, move ->
            val count = move.movement.count
            val type = move.movement.movementType

            when (type) {
                MovementType.INCOMING -> acc.copy(incoming = acc.incoming + count)
                MovementType.OUTGOING -> acc.copy(outgoing = acc.outgoing + count)
            }
        }
    }

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
     * 1. SQL фильтрует движения по диапазону start..end
     * 2. SQL рассчитывает начальный баланс до start
     * 3. Kotlin суммирует incoming/outgoing для каждой партии
     *
     * @param start Начало периода (включительно для движений, не включительно для начального баланса)
     * @param end Конец периода (включительно)
     * @return Flow со списком продуктов, отсортированных по названию
     */
    @Suppress("LongMethod")
    fun observeOnStorageBatches(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<StorageProduct>> {
        require(start <= end) { "start must be before or equal to end, got start=$start, end=$end" }
        return combine(
            observeMovementsInRange(start, end),
            observeInitialBalanceForAllBatches(start).toBatchIdMap()
        ) { movementsInRange, initialBalances ->
            // Группируем движения по продукту
            movementsInRange
                .groupBy { it.batchOut.product }
                .entries
                .mapParallel(Dispatchers.Default) { (product, movementOuts) ->
                    // Извлекаем информацию о продукте
                    val productId = product.id
                    val productName = product.displayName

                    // Группируем движения по партиям и рассчитываем баланс для каждой
                    val batches = movementOuts
                        .groupBy { it.batchOut.batch }
                        .entries
                        .sortedBy { it.key.dateBorn }
                        .map { (batch: ru.pavlig43.database.data.batch.BatchBD, moves) ->
                            val batchId = batch.id
                            val batchName =
                                "($batchId) ${batch.dateBorn.format(dateFormat)}"

                            // Получаем начальный баланс из SQL запроса
                            val balanceBeforeStart = initialBalances[batchId] ?: 0
                            // Рассчитываем incoming/outgoing за период
                            val calculation = moves.calculateMovementBalance()

                            StorageBatch(
                                batchId = batchId,
                                batchName = batchName,
                                balanceBeforeStart = balanceBeforeStart,
                                incoming = calculation.incoming,
                                outgoing = calculation.outgoing,
                                balanceOnEnd = balanceBeforeStart + calculation.incoming - calculation.outgoing
                            )
                        }

                    // Суммируем данные по всем партиям продукта
                    val totals =
                        batches.fold(MovementBalanceCalculation.ZERO) { acc, batch ->
                            MovementBalanceCalculation(
                                balanceBefore = acc.balanceBefore + batch.balanceBeforeStart,
                                incoming = acc.incoming + batch.incoming,
                                outgoing = acc.outgoing + batch.outgoing
                            )
                        }

                    StorageProduct(
                        productId = productId,
                        productName = productName,
                        balanceBeforeStart = totals.balanceBefore,
                        incoming = totals.incoming,
                        outgoing = totals.outgoing,
                        balanceOnEnd = totals.balanceBefore + totals.incoming - totals.outgoing,
                        batches = batches
                    )
                }
                .sortedBy { it.productName }
        }
    }

    /**
     * Наблюдает за движениями конкретной партии в указанном диапазоне.
     *
     * Движения отсортированы по дате создания транзакции.
     *
     * @param batchId ID партии
     * @param start Начало периода (включительно)
     * @param end Конец периода (включительно)
     * @return Flow со списком движений партии, отсортированных по дате
     */
    @Transaction
    @Query(
        """
        SELECT bm.* FROM batch_movement bm
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE bm.batch_id = :batchId
        AND t.created_at >= :start
        AND t.created_at <= :end
        ORDER BY t.created_at
        """
    )
    internal abstract fun observeMovementsByBatchIdSorted(
        batchId: Int,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<MovementOut>>

    /**
     * Наблюдает за начальным балансом партии до указанной даты.
     *
     * Рассчитывает сумму всех движений партии до [start]:
     * - INCOMING: +count
     * - OUTGOING: -count
     *
     * @param batchId ID партии
     * @param start Дата, до которой учитываются движения (не включительно)
     * @return Flow с начальным балансом партии
     */
    @Transaction
    @Query(
        """
        SELECT
        COALESCE(SUM(CASE WHEN bm.movement_type = 'INCOMING' THEN bm.count ELSE -bm.count END), 0) as balance
        FROM batch_movement bm
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE bm.batch_id = :batchId
        AND t.created_at < :start
    """
    )
    internal abstract fun observeInitialBalanceForBatch(
        batchId: Int,
        start: LocalDateTime
    ): Flow<List<SingleBatchBalanceBD>>

    /**
     * Наблюдает за движениями партии с накопительным балансом.
     *
     * Для каждого движения рассчитывает:
     * - balanceBeforeStart: баланс ДО этого движения
     * - incoming/outgoing: количество в этом движении
     * - balanceOnEnd: баланс ПОСЛЕ этого движения
     *
     * Архитектура:
     * 1. SQL фильтрует движения по диапазону start..end и сортирует по дате
     * 2. SQL рассчитывает начальный баланс до [start]
     * 3. Kotlin рассчитывает накопительный баланс для каждого движения
     *
     * @param batchId ID партии
     * @param start Начало периода (включительно для движений, не включительно для начального баланса)
     * @param end Конец периода (включительно)
     * @return Flow с информацией о партии и списках движений с балансами
     */
    fun observeBatchMovementsWithBalance(
        batchId: Int,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<BatchMovementWithBalanceInfoBD> {
        require(start <= end) { "start must be before or equal to end, got start=$start, end=$end" }
        return combine(
            observeMovementsByBatchIdSorted(batchId, start, end),
            observeInitialBalanceForBatch(batchId, start).toInt()
        ) { movements, initialBalance ->
            // Рассчитываем накопительный баланс для каждого движения
            val movementsWithBalance = buildList {
                var currentBalance = initialBalance
                for (movementOut in movements) {
                    val count = movementOut.movement.count
                    val incoming = if (movementOut.movement.movementType == MovementType.INCOMING) count else 0
                    val outgoing = if (movementOut.movement.movementType == MovementType.OUTGOING) count else 0

                    add(
                        BatchMovementWithBalanceBD(
                            movementDate = movementOut.transaction.createdAt,
                            balanceBeforeStart = currentBalance,
                            incoming = incoming,
                            outgoing = outgoing,
                            balanceOnEnd = currentBalance + incoming - outgoing,
                            transactionId = movementOut.movement.transactionId
                        )
                    )

                    currentBalance += incoming - outgoing
                }
            }

            // Извлекаем название продукта из первого движения
            // Если движений нет, возвращаем пустой результат
            if (movements.isEmpty()) {
                return@combine BatchMovementWithBalanceInfoBD(
                    batchId = batchId,
                    productName = "",
                    movements = emptyList()
                )
            }

            val firstMovement = movements.first()
            val productName = firstMovement.batchOut.product.displayName

            BatchMovementWithBalanceInfoBD(
                batchId = batchId,
                productName = productName,
                movements = movementsWithBalance
            )
        }
    }
}

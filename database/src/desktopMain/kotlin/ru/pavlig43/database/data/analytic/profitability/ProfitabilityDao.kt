package ru.pavlig43.database.data.analytic.profitability

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.transact.sale.dao.InternalSale

@Dao
interface ProfitabilityDao {

    @Transaction
    @Query(
        """
     SELECT * FROM sale
     WHERE transaction_id IN (
         SELECT id FROM transact
         WHERE created_at >= :start AND created_at <= :end
     )
     """
    )
    fun observeOnSale(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<InternalSale>>

    @Query(
        """
        SELECT COALESCE(SUM((bm.count * COALESCE(bcp.cost_price_per_unit, 0)) / 1000), 0)
        FROM batch_movement AS bm
        JOIN transact AS t ON t.id = bm.transaction_id
        LEFT JOIN batch_cost_price AS bcp
          ON bcp.batch_id = bm.batch_id AND bcp.deleted_at IS NULL
        WHERE t.transaction_type = 'WRITE_OFF'
          AND t.created_at >= :start AND t.created_at <= :end
          AND t.deleted_at IS NULL
          AND bm.movement_type = 'OUTGOING'
          AND bm.deleted_at IS NULL
        """
    )
    fun observeMaterialWriteOffCost(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<Long>

}




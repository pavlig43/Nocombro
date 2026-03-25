package ru.pavlig43.database.data.analytic.profitability

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.transact.sale.dao.InternalSale

@Dao
abstract class ProfitabilityDao {

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
    abstract fun observeOnSale(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<InternalSale>>

}




package ru.pavlig43.database.data.analytic.profitability

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.mapParallel
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.transact.sale.dao.InternalSale

@Dao
abstract class ProfitabilityDao {

    @Transaction
    @Query(
        """
        SELECT * FROM sale s
        JOIN transact t ON s.transaction_id = t.id
        WHERE t.created_at >= :start AND t.created_at <= :end
    """
    )
    internal abstract fun observeOnSale(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<InternalSale>>

    fun observeOnProductSale(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<ProfitabilityBD>> {
        return observeOnSale(start, end).map { lst ->
            lst.groupBy { it.movementOut.batchOut.product.id }
                .also {
                    it.forEach { mp ->
                        println(mp)
                        println("___________________")
                    }
                }
                .values.mapParallel(Dispatchers.IO) { sales ->
                    val details = sales.map { sale ->
                        ProfitabilityDetails(
                            contrAgentId = sale.client.id,
                            contrAgentName = sale.client.displayName,
                            date = sale.transaction.createdAt,
                            quantity = sale.movementOut.movement.count,
                            revenue = sale.sale.price,
                            expenses = 0,
                            expensesOnOneKg = 0,
                            profit = 0,
                            margin = 0.0,
                            profitability = 0.0
                        )
                    }

                    ProfitabilityBD(
                        productId = sales.first().movementOut.batchOut.product.id,
                        productName = sales.first().movementOut.batchOut.product.displayName,
                        quantity = details.sumOf { it.quantity },
                        revenue = details.sumOf { it.revenue },
                        expenses = 0,
                        expensesOnOneKg = 0,
                        profit = 0,
                        margin = 0.0,
                        profitability = 0.0,
                        details = details
                    )
                }
        }
    }
}

data class ProfitabilityBD(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val revenue: Int,
    val expenses: Int,
    val expensesOnOneKg: Int,
    val profit: Int,
    val margin: Double,
    val profitability: Double,
    val details: List<ProfitabilityDetails>
)

data class ProfitabilityDetails(
    val contrAgentId: Int,
    val contrAgentName: String,
    val date: LocalDateTime,
    val quantity: Int,
    val revenue: Int,
    val expenses: Int,
    val expensesOnOneKg: Int,
    val profit: Int,
    val margin: Double,
    val profitability: Double
)


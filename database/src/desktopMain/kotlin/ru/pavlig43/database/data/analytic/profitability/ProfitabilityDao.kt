package ru.pavlig43.database.data.analytic.profitability

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.mapParallel
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.transact.sale.SaleBDOut

@Dao
abstract class ProfitabilityDao {


    @Query(
        """
        SELECT * FROM sale s
        JOIN transact t ON s.transaction_id = t.id
        WHERE t.created_at >= :start AND t.created_at <= :end
    """
    )
    internal abstract fun observeOnSale(start: LocalDateTime, end: LocalDateTime): Flow<List<InternalProfitabilityBD>>

    fun observeOnProductSale(start: LocalDateTime, end: LocalDateTime): Flow<List<ProfitabilityBD>> {
        val a = observeOnSale(start, end).map { lst ->
            lst.groupBy { it.movementOut.batchOut.product.id }.values.mapParallel(Dispatchers.IO) { lst: List<InternalProfitabilityBD> ->
                lst.mapParallel { prof: InternalProfitabilityBD ->
                    val contrAgentId = prof.sale.clientId
                    val contrAgentName = prof.sale.clientName
                    val date = prof.movementOut.transaction.createdAt
                    val quantity = prof.sale.count
                    val revenue = prof.sale.price
                    val prod = prof.movementOut.transaction.transactionType
//                    val expenses=
//                    val expensesOnOneKg=
//                    val profit=
//                    val margin=
//                    val profitability=                 }
                }
            }

        }
        return flowOf(emptyList())
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

internal data class InternalProfitabilityBD(
    @Embedded
    val sale: SaleBDOut,
    @Relation(
        entity = BatchMovement::class,
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val movementOut: MovementOut

)
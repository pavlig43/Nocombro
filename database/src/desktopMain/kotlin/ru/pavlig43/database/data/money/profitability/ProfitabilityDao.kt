package ru.pavlig43.database.data.money.profitability

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.mapParallel
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.transact.sale.SaleBDIn

@Dao
abstract class ProfitabilityDao {


    @Query("""
        SELECT * FROM sale s
        JOIN transact t ON s.transaction_id = t.id
        WHERE t.created_at >= :start AND t.created_at <= :end
    """)
    internal abstract fun observeOnSale(start: LocalDateTime,end: LocalDateTime): Flow<List<InternalProfitabilityBD>>

//    fun observeOnProductSale(start: LocalDateTime,end: LocalDateTime): Flow<List<ProfitabilityBD>>{
//        val a = observeOnSale(start,end).map { lst->
//            lst.groupBy { it.movementOut.batchOut.product.id }.values.mapParallel { values: List<InternalProfitabilityBD> ->
//
//            }
//        }
//    }
}


data class ProfitabilityBD(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val revenue: Int
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
    val sale: SaleBDIn,
    @Relation(
        entity = BatchMovement::class,
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val movementOut: MovementOut

)
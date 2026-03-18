package ru.pavlig43.database.data.money.profitability

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
abstract class ProfitabilityDao {

    @Query("""
        SELECT
            p.id as productId,
            p.display_name as productName,
            SUM(bm.count) as quantity,
            SUM(s.price * bm.count) as revenue
        FROM sale s
        INNER JOIN batch_movement bm ON s.movement_id = bm.id
        INNER JOIN batch bd ON bm.batch_id = bd.id
        INNER JOIN product p ON bd.product_id = p.id
        INNER JOIN transact t ON s.transaction_id = t.id
        WHERE t.created_at >= :start AND t.created_at <= :end
        GROUP BY p.id, p.display_name
    """)
    abstract fun observeProductSales(start: LocalDateTime, end: LocalDateTime): Flow<List<ProductSalesBD>>
}
package ru.pavlig43.database.data.transact.sale.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.sale.SALE_TABLE_NAME
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.transact.sale.SaleBDOut
import ru.pavlig43.database.data.vendor.Vendor

@Dao
abstract class SaleDao {

    @Transaction
    @Query("SELECT * FROM $SALE_TABLE_NAME WHERE transaction_id = :transactionId ORDER BY id DESC")
    internal abstract suspend fun getSalesWithRelations(transactionId: Int): List<InternalSale>

    suspend fun getSalesWithDetails(transactionId: Int): List<SaleBDOut> {
        return getSalesWithRelations(transactionId).map(InternalSale::toSaleBDOut)
    }

    @Upsert
    abstract suspend fun upsertSaleBd(sale: SaleBDIn)

    @Query("DELETE FROM $SALE_TABLE_NAME WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT movement_id FROM $SALE_TABLE_NAME WHERE id IN (:ids)")
    abstract suspend fun getMovementIdsBySaleIds(ids: List<Int>): List<Int>
}

internal data class InternalSale(
    @Embedded
    val sale: SaleBDIn,
    @Relation(
        entity = Transact::class,
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val transaction: Transact,
    @Relation(
        entity = BatchMovement::class,
        parentColumn = "movement_id",
        entityColumn = "id",
    )
    val movementOut: MovementOut,
    @Relation(
        entity = Vendor::class,
        parentColumn = "client_id",
        entityColumn = "id"
    )
    val client: Vendor
)


private fun InternalSale.toSaleBDOut(): SaleBDOut {
    val batchOut = movementOut.batchOut

    return SaleBDOut(
        transactionId = transaction.id,
        productId = batchOut.product.id,
        productName = batchOut.product.displayName,
        dateBorn = batchOut.batch.dateBorn,
        count = movementOut.movement.count,
        batchId = movementOut.movement.batchId,
        vendorName = batchOut.declaration.vendorName,
        clientName = client.displayName,
        clientId = client.id,
        price = sale.price,
        comment = sale.comment,
        id = sale.id,
        movementId = movementOut.movement.id
    )
}

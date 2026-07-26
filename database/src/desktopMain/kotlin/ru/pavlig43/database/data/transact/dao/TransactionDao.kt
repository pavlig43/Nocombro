package ru.pavlig43.database.data.transact.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.batch.BATCH_MOVEMENT_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_TABLE_NAME
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.transact.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionCounterpartyName
import ru.pavlig43.database.data.transact.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transact.sale.SALE_TABLE_NAME
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME

@Dao
interface TransactionDao {


@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun create(transaction: Transact): Long

    @Update
    suspend fun updateTransaction(transaction: Transact)

    @Query("DELETE FROM $TRANSACTION_TABLE_NAME WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: Set<Int>)

    @Query("SELECT * FROM  $TRANSACTION_TABLE_NAME WHERE id = :id")
    suspend fun getTransaction(id: Int): Transact

    @Query("SELECT * FROM $TRANSACTION_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getTransactionBySyncId(syncId: String): Transact?

    @Query("SELECT * FROM $TRANSACTION_TABLE_NAME")
    suspend fun getAll(): List<Transact>

    @Query("""
    SELECT * FROM $TRANSACTION_TABLE_NAME
    ORDER BY created_at DESC
""")
    fun observeOnProductTransactions(): Flow<List<Transact>>

    @Query(
        """
        SELECT buy.transaction_id AS transaction_id,
               declaration.vendor_name AS counterparty_name
        FROM $BUY_TABLE_NAME AS buy
        INNER JOIN $BATCH_MOVEMENT_TABLE_NAME AS movement ON movement.id = buy.movement_id
        INNER JOIN $BATCH_TABLE_NAME AS batch ON batch.id = movement.batch_id
        INNER JOIN $DECLARATIONS_TABLE_NAME AS declaration ON declaration.id = batch.declaration_id

        UNION ALL

        SELECT sale.transaction_id AS transaction_id,
               client.display_name AS counterparty_name
        FROM $SALE_TABLE_NAME AS sale
        INNER JOIN $VENDOR_TABLE_NAME AS client ON client.id = sale.client_id
        """
    )
    fun observeCounterpartyNames(): Flow<List<TransactionCounterpartyName>>


    //TODO сделать проверку транзакций
    suspend fun isCanSave(transaction: Transact): Result<Unit> {
        return Result.success(Unit)
    }

}


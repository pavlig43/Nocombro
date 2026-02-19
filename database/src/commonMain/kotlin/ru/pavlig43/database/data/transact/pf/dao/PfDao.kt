package ru.pavlig43.database.data.transact.pf.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.transact.pf.PF_TABLE_NAME
import ru.pavlig43.database.data.transact.pf.PfBD

@Dao
interface PfDao {

    @Query("SELECT * FROM $PF_TABLE_NAME WHERE transaction_id = :transactionId")
    suspend fun getByTransactionId(transactionId: Int): PfBD?

    @Upsert
    suspend fun upsert(pf: PfBD): Long
}

package ru.pavlig43.database.data.transaction.reminder.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.transaction.reminder.ReminderBD
import ru.pavlig43.database.data.transaction.reminder.REMINDER_TABLE_NAME

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderBD): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reminders: List<ReminderBD>)

    @Query("DELETE FROM $REMINDER_TABLE_NAME WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("DELETE FROM $REMINDER_TABLE_NAME WHERE transaction_id = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Int)

    @Query("SELECT * FROM $REMINDER_TABLE_NAME WHERE transaction_id = :transactionId ORDER BY reminder_date_time ASC")
    fun observeByTransactionId(transactionId: Int): Flow<List<ReminderBD>>

    @Query("SELECT * FROM $REMINDER_TABLE_NAME WHERE id = :id")
    suspend fun getById(id: Int): ReminderBD?

    @Query("SELECT * FROM $REMINDER_TABLE_NAME WHERE transaction_id = :transactionId")
    suspend fun getByTransactionId(transactionId: Int): List<ReminderBD>
}

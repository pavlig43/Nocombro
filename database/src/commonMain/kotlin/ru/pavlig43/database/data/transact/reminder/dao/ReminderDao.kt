package ru.pavlig43.database.data.transact.reminder.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.transact.reminder.REMINDER_TABLE_NAME
import ru.pavlig43.database.data.transact.reminder.ReminderBD

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reminders: List<ReminderBD>)

    @Query("DELETE FROM $REMINDER_TABLE_NAME WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)


    @Query("SELECT * FROM $REMINDER_TABLE_NAME WHERE transaction_id = :transactionId")
    suspend fun getByTransactionId(transactionId: Int): List<ReminderBD>

    /**
     * Получает все напоминания для уведомлений
     */
    @Query("SELECT * FROM $REMINDER_TABLE_NAME")
    fun observeAllReminders(): Flow<List<ReminderBD>>

    /**
     * Получает просроченные и сегодняшние напоминания
     */
    fun observeTodayReminders(): Flow<List<NotificationDTO>> {
        return observeAllReminders().map { lst ->
            val today = getCurrentLocalDate()
            lst.filter { it.reminderDateTime.date <= today }.map {

                NotificationDTO(
                    id = it.transactionId,
                    displayName = it.text
                )
            }
        }
    }
}

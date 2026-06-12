package ru.pavlig43.database.data.experiment.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.experiment.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_TABLE_NAME
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.datetime.getCurrentLocalDate

@Dao
interface ExperimentReminderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(reminder: ExperimentReminder): Long

    @Upsert
    suspend fun upsert(reminder: ExperimentReminder)

    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE id = :id")
    suspend fun getReminder(id: Int): ExperimentReminder?

    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getReminderBySyncId(syncId: String): ExperimentReminder?

    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME")
    suspend fun getAll(): List<ExperimentReminder>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY reminder_date_time ASC, id ASC
        """
    )
    fun observeReminders(experimentId: Int): Flow<List<ExperimentReminder>>

    @Query(
        """
        SELECT r.experiment_id AS id, e.title || ': ' || r.text AS displayName, r.reminder_date_time AS reminderDateTime
        FROM $EXPERIMENT_REMINDER_TABLE_NAME r
        INNER JOIN $EXPERIMENT_TABLE_NAME e ON e.id = r.experiment_id
        WHERE r.deleted_at IS NULL AND e.deleted_at IS NULL
        """
    )
    fun observeAllNotificationCandidates(): Flow<List<ExperimentReminderNotificationRow>>

    fun observeTodayReminders(): Flow<List<NotificationDTO>> {
        return observeAllNotificationCandidates().map { items ->
            val today = getCurrentLocalDate()
            items.filter { notification ->
                notification.reminderDateTime.date <= today
            }.map { notification ->
                NotificationDTO(
                    id = notification.id,
                    displayName = notification.displayName,
                )
            }
        }
    }
}

data class ExperimentReminderNotificationRow(
    val id: Int,
    val displayName: String,
    val reminderDateTime: LocalDateTime,
)

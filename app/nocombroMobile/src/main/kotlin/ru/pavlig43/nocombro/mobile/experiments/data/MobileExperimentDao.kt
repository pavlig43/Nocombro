package ru.pavlig43.nocombro.mobile.experiments.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface MobileExperimentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(experiment: MobileExperimentEntity): Long

    @Upsert
    suspend fun upsert(experiment: MobileExperimentEntity)

    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME WHERE id = :id")
    suspend fun getExperiment(id: Int): MobileExperimentEntity?

    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentEntity>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_TABLE_NAME
        WHERE deleted_at IS NULL AND is_archived = :isArchived
        ORDER BY updated_at DESC, id DESC
        """
    )
    fun observeExperiments(isArchived: Boolean): Flow<List<MobileExperimentEntity>>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_TABLE_NAME
        WHERE id = :id AND deleted_at IS NULL
        """
    )
    fun observeExperiment(id: Int): Flow<MobileExperimentEntity?>
}

@Dao
interface MobileExperimentEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(entry: MobileExperimentEntryEntity): Long

    @Upsert
    suspend fun upsert(entry: MobileExperimentEntryEntity)

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE id = :id")
    suspend fun getEntry(id: Int): MobileExperimentEntryEntity?

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentEntryEntity>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE experiment_id = :experimentId AND entry_date = :entryDate
        LIMIT 1
        """
    )
    suspend fun getEntryByExperimentAndDate(
        experimentId: Int,
        entryDate: LocalDate,
    ): MobileExperimentEntryEntity?

    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY entry_date DESC, id DESC
        """
    )
    fun observeEntries(experimentId: Int): Flow<List<MobileExperimentEntryEntity>>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE id = :id AND deleted_at IS NULL
        """
    )
    fun observeEntry(id: Int): Flow<MobileExperimentEntryEntity?>
}

@Dao
interface MobileExperimentReminderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(reminder: MobileExperimentReminderEntity): Long

    @Upsert
    suspend fun upsert(reminder: MobileExperimentReminderEntity)

    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE id = :id")
    suspend fun getReminder(id: Int): MobileExperimentReminderEntity?

    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentReminderEntity>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY reminder_date_time ASC, id ASC
        """
    )
    fun observeReminders(experimentId: Int): Flow<List<MobileExperimentReminderEntity>>
}

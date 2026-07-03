package ru.pavlig43.nocombro.mobile.internal.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.nocombro.mobile.internal.database.entity.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.nocombro.mobile.internal.database.entity.EXPERIMENT_ENTRY_FILE_TABLE_NAME
import ru.pavlig43.nocombro.mobile.internal.database.entity.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.nocombro.mobile.internal.database.entity.EXPERIMENT_TABLE_NAME
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryFileEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentReminderEntity

/**
 * DAO for mobile experiment metadata.
 */
@Dao
interface MobileExperimentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(experiment: MobileExperimentEntity): Long

    @Upsert
    suspend fun upsert(experiment: MobileExperimentEntity)

    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME WHERE id = :id")
    suspend fun getExperiment(id: Int): MobileExperimentEntity?

    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getExperimentBySyncId(syncId: String): MobileExperimentEntity?

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

/**
 * DAO for mobile experiment journal entries.
 */
@Dao
interface MobileExperimentEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(entry: MobileExperimentEntryEntity): Long

    @Upsert
    suspend fun upsert(entry: MobileExperimentEntryEntity)

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE id = :id")
    suspend fun getEntry(id: Int): MobileExperimentEntryEntity?

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getEntryBySyncId(syncId: String): MobileExperimentEntryEntity?

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentEntryEntity>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY entry_date DESC, created_at DESC, id DESC
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

/**
 * DAO for files attached to mobile experiment entries.
 */
@Dao
interface MobileExperimentEntryFileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(file: MobileExperimentEntryFileEntity): Long

    @Upsert
    suspend fun upsert(file: MobileExperimentEntryFileEntity)

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_FILE_TABLE_NAME WHERE id = :id")
    suspend fun getFile(id: Int): MobileExperimentEntryFileEntity?

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_FILE_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentEntryFileEntity>

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_FILE_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getFileBySyncId(syncId: String): MobileExperimentEntryFileEntity?

    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_FILE_TABLE_NAME
        WHERE entry_id = :entryId AND deleted_at IS NULL
        ORDER BY updated_at DESC, id DESC
        """
    )
    fun observeFiles(entryId: Int): Flow<List<MobileExperimentEntryFileEntity>>
}

/**
 * DAO for mobile experiment reminders.
 */
@Dao
interface MobileExperimentReminderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(reminder: MobileExperimentReminderEntity): Long

    @Upsert
    suspend fun upsert(reminder: MobileExperimentReminderEntity)

    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE id = :id")
    suspend fun getReminder(id: Int): MobileExperimentReminderEntity?

    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getReminderBySyncId(syncId: String): MobileExperimentReminderEntity?

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

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

    /**
     * Наблюдает эксперименты нужного типа архива в порядке последней активности.
     *
     * В расчёт входят версии самого эксперимента, записей, напоминаний и файлов.
     * Tombstone не фильтруются во вложенных запросах: их версия тоже должна поднять
     * недавно изменённый эксперимент вверх до завершения синхронизации.
     *
     * @param isArchived требуемое состояние архива.
     */
    @Query(
        """
        SELECT e.* FROM $EXPERIMENT_TABLE_NAME e
        WHERE e.deleted_at IS NULL AND e.is_archived = :isArchived
        ORDER BY MAX(
            e.updated_at,
            COALESCE((
                SELECT MAX(ee.updated_at) FROM $EXPERIMENT_ENTRY_TABLE_NAME ee
                WHERE ee.experiment_id = e.id
            ), e.updated_at),
            COALESCE((
                SELECT MAX(er.updated_at) FROM $EXPERIMENT_REMINDER_TABLE_NAME er
                WHERE er.experiment_id = e.id
            ), e.updated_at),
            COALESCE((
                SELECT MAX(ef.updated_at)
                FROM $EXPERIMENT_ENTRY_FILE_TABLE_NAME ef
                INNER JOIN $EXPERIMENT_ENTRY_TABLE_NAME ee2 ON ee2.id = ef.entry_id
                WHERE ee2.experiment_id = e.id
            ), e.updated_at)
        ) DESC, e.id DESC
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

    /**
     * Возвращает все записи эксперимента, включая tombstone.
     *
     * Полный набор нужен для каскадной пометки удаления и расчёта старшей версии.
     */
    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE experiment_id = :experimentId")
    suspend fun getEntriesByExperiment(experimentId: Int): List<MobileExperimentEntryEntity>

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

    /**
     * Возвращает все файлы переданных записей, включая tombstone.
     *
     * @param entryIds непустой список локальных идентификаторов записей.
     */
    @Query("SELECT * FROM $EXPERIMENT_ENTRY_FILE_TABLE_NAME WHERE entry_id IN (:entryIds)")
    suspend fun getFilesByEntries(entryIds: List<Int>): List<MobileExperimentEntryFileEntity>

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

    /**
     * Возвращает все напоминания эксперимента, включая tombstone.
     *
     * Полный набор нужен для каскадной пометки удаления и расчёта старшей версии.
     */
    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE experiment_id = :experimentId")
    suspend fun getRemindersByExperiment(experimentId: Int): List<MobileExperimentReminderEntity>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY reminder_date_time ASC, id ASC
        """
    )
    fun observeReminders(experimentId: Int): Flow<List<MobileExperimentReminderEntity>>
}

package ru.pavlig43.nocombro.mobile.experiments.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * DAO для таблицы экспериментов.
 */
@Dao
interface MobileExperimentDao {
    /**
     * Создаёт эксперимент и возвращает локальный id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(experiment: MobileExperimentEntity): Long

    /**
     * Вставляет или обновляет эксперимент.
     */
    @Upsert
    suspend fun upsert(experiment: MobileExperimentEntity)

    /**
     * Возвращает эксперимент по локальному id.
     */
    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME WHERE id = :id")
    suspend fun getExperiment(id: Int): MobileExperimentEntity?

    /**
     * Возвращает все строки экспериментов, включая tombstone.
     */
    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentEntity>

    /**
     * Следит за активными экспериментами в выбранном архивном состоянии.
     */
    @Query(
        """
        SELECT * FROM $EXPERIMENT_TABLE_NAME
        WHERE deleted_at IS NULL AND is_archived = :isArchived
        ORDER BY updated_at DESC, id DESC
        """
    )
    fun observeExperiments(isArchived: Boolean): Flow<List<MobileExperimentEntity>>

    /**
     * Следит за активным экспериментом по локальному id.
     */
    @Query(
        """
        SELECT * FROM $EXPERIMENT_TABLE_NAME
        WHERE id = :id AND deleted_at IS NULL
        """
    )
    fun observeExperiment(id: Int): Flow<MobileExperimentEntity>
}

/**
 * DAO для записей журнала эксперимента.
 */
@Dao
interface MobileExperimentEntryDao {
    /**
     * Создаёт запись журнала и возвращает локальный id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(entry: MobileExperimentEntryEntity): Long

    /**
     * Вставляет или обновляет запись журнала.
     */
    @Upsert
    suspend fun upsert(entry: MobileExperimentEntryEntity)

    /**
     * Возвращает запись журнала по локальному id.
     */
    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE id = :id")
    suspend fun getEntry(id: Int): MobileExperimentEntryEntity?

    /**
     * Возвращает все записи журнала, включая tombstone.
     */
    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentEntryEntity>

    /**
     * Возвращает запись эксперимента за дату, если она уже есть.
     */
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

    /**
     * Следит за активными записями выбранного эксперимента.
     */
    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY entry_date DESC, id DESC
        """
    )
    fun observeEntries(experimentId: Int): Flow<List<MobileExperimentEntryEntity>>

    /**
     * Следит за активной записью журнала по локальному id.
     */
    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE id = :id AND deleted_at IS NULL
        """
    )
    fun observeEntry(id: Int): Flow<MobileExperimentEntryEntity?>
}

/**
 * DAO для напоминаний по эксперименту.
 */
@Dao
interface MobileExperimentReminderDao {
    /**
     * Создаёт напоминание и возвращает локальный id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(reminder: MobileExperimentReminderEntity): Long

    /**
     * Вставляет или обновляет напоминание.
     */
    @Upsert
    suspend fun upsert(reminder: MobileExperimentReminderEntity)

    /**
     * Возвращает напоминание по локальному id.
     */
    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE id = :id")
    suspend fun getReminder(id: Int): MobileExperimentReminderEntity?

    /**
     * Возвращает все напоминания, включая tombstone.
     */
    @Query("SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME")
    suspend fun getAll(): List<MobileExperimentReminderEntity>

    /**
     * Следит за активными напоминаниями выбранного эксперимента.
     */
    @Query(
        """
        SELECT * FROM $EXPERIMENT_REMINDER_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY reminder_date_time ASC, id ASC
        """
    )
    fun observeReminders(experimentId: Int): Flow<List<MobileExperimentReminderEntity>>
}

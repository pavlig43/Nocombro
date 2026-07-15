package ru.pavlig43.database.data.experiment.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.experiment.EXPERIMENT_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.files.FILE_TABLE_NAME

@Dao
interface ExperimentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(experiment: Experiment): Long

    @Upsert
    suspend fun upsert(experiment: Experiment)

    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME WHERE id = :id")
    suspend fun getExperiment(id: Int): Experiment?

    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getExperimentBySyncId(syncId: String): Experiment?

    @Query("SELECT * FROM $EXPERIMENT_TABLE_NAME")
    suspend fun getAll(): List<Experiment>

    /**
     * Наблюдает активные или архивные эксперименты в порядке последней активности.
     *
     * Порядок учитывает версии самого эксперимента, записей, напоминаний и файлов
     * записей. Поэтому правка или tombstone дочерней строки поднимает эксперимент
     * вверх без искусственного изменения версии родителя.
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
                SELECT MAX(f.updated_at)
                FROM $FILE_TABLE_NAME f
                INNER JOIN $EXPERIMENT_ENTRY_TABLE_NAME efe ON efe.id = f.owner_id
                WHERE f.owner_type = 'EXPERIMENT_ENTRY' AND efe.experiment_id = e.id
            ), e.updated_at)
        ) DESC, e.id DESC
        """
    )
    fun observeExperiments(isArchived: Boolean): Flow<List<Experiment>>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_TABLE_NAME
        WHERE id = :id AND deleted_at IS NULL
        """
    )
    fun observeExperiment(id: Int): Flow<Experiment?>
}

package ru.pavlig43.database.data.experiment.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.experiment.EXPERIMENT_TABLE_NAME
import ru.pavlig43.database.data.experiment.Experiment

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

    @Query(
        """
        SELECT * FROM $EXPERIMENT_TABLE_NAME
        WHERE deleted_at IS NULL AND is_archived = :isArchived
        ORDER BY updated_at DESC, id DESC
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

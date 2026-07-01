package ru.pavlig43.database.data.experiment.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.ExperimentEntry

@Dao
interface ExperimentEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(entry: ExperimentEntry): Long

    @Upsert
    suspend fun upsert(entry: ExperimentEntry)

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE id = :id")
    suspend fun getEntry(id: Int): ExperimentEntry?

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getEntryBySyncId(syncId: String): ExperimentEntry?

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME")
    suspend fun getAll(): List<ExperimentEntry>

    @Query("SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE experiment_id = :experimentId")
    suspend fun getEntriesByExperiment(experimentId: Int): List<ExperimentEntry>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE experiment_id = :experimentId AND deleted_at IS NULL
        ORDER BY entry_date DESC, created_at DESC, id DESC
        """
    )
    fun observeEntries(experimentId: Int): Flow<List<ExperimentEntry>>

    @Query(
        """
        SELECT * FROM $EXPERIMENT_ENTRY_TABLE_NAME
        WHERE id = :id AND deleted_at IS NULL
        """
    )
    fun observeEntry(id: Int): Flow<ExperimentEntry?>
}

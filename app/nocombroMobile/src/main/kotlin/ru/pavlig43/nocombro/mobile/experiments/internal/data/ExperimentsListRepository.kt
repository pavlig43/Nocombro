package ru.pavlig43.nocombro.mobile.experiments.internal.data

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperiment
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.experiments.data.toModel

class ExperimentsListRepository(
    db: MobileExperimentsDatabase,
) {
    private val experimentDao = db.experimentDao

    fun observeActiveExperiments(): Flow<List<MobileExperiment>> {
        return experimentDao.observeExperiments(isArchived = false)
            .map { list -> list.map(MobileExperimentEntity::toModel) }
    }

    fun observeArchivedExperiments(): Flow<List<MobileExperiment>> {
        return experimentDao.observeExperiments(isArchived = true)
            .map { list -> list.map(MobileExperimentEntity::toModel) }
    }

    fun observeExperiment(id: Int): Flow<MobileExperiment?> {
        return experimentDao.observeExperiment(id)
            .map { experiment -> experiment?.toModel() }
    }

    suspend fun createAndReturnExperiment(): Result<MobileExperiment> {
        return runCatching {
            val experiment = MobileExperimentEntity(
                title = "",
                syncId = UUID.randomUUID().toString(),
                updatedAt = getCurrentLocalDateTime(),
            )
            val id = experimentDao.create(experiment).toInt()
            experiment.copy(id = id).toModel()
        }
    }

    suspend fun deleteExperiment(id: Int): Result<Unit> {
        return runCatching {
            val experiment = requireExperiment(id)
            val deletedAt = getCurrentLocalDateTime()
            experimentDao.upsert(
                experiment.copy(
                    updatedAt = deletedAt,
                    deletedAt = deletedAt,
                )
            )
        }
    }

    suspend fun setExperimentArchived(
        id: Int,
        isArchived: Boolean,
    ): Result<Unit> {
        return runCatching {
            val experiment = requireExperiment(id)
            experimentDao.upsert(
                experiment.copy(
                    isArchived = isArchived,
                    updatedAt = getCurrentLocalDateTime(),
                )
            )
        }
    }

    private suspend fun requireExperiment(id: Int): MobileExperimentEntity {
        return requireNotNull(experimentDao.getExperiment(id)) {
            "Experiment $id not found"
        }
    }
}

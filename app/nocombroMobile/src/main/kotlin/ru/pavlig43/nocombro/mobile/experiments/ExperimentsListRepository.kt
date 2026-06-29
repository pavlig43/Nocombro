package ru.pavlig43.nocombro.mobile.experiments

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.experiments.data.toModel

/**
 * Репозиторий списка mobile-экспериментов.
 */
class ExperimentsListRepository(
    db: MobileExperimentsDatabase,
) {
    private val experimentDao = db.experimentDao

    /**
     * Следит за живыми неархивными экспериментами.
     */
    fun observeActiveExperiments(): Flow<List<MobileExperiment>> {
        return experimentDao.observeExperiments(isArchived = false)
            .map { list -> list.map(MobileExperimentEntity::toModel) }
    }

    /**
     * Следит за живыми архивными экспериментами.
     */
    fun observeArchivedExperiments(): Flow<List<MobileExperiment>> {
        return experimentDao.observeExperiments(isArchived = true)
            .map { list -> list.map(MobileExperimentEntity::toModel) }
    }

    /**
     * Следит за живым экспериментом по локальному id.
     */
    fun observeExperiment(id: Int): Flow<MobileExperiment> {
        return experimentDao.observeExperiment(id)
            .map(MobileExperimentEntity::toModel)
    }

    /**
     * Создаёт новый эксперимент.
     */
    suspend fun createExperiment(): Result<MobileExperiment> {
        return runCatching {
            val experiment = MobileExperimentEntity(
                title = "Новый эксперимент",
                syncId = UUID.randomUUID().toString(),
                updatedAt = getCurrentLocalDateTime(),
            )
            val id = experimentDao.create(experiment).toInt()
            experiment.copy(id = id).toModel()
        }
    }

    /**
     * Помечает эксперимент удалённым.
     */
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

    /**
     * Меняет архивный флаг эксперимента.
     */
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

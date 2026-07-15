package ru.pavlig43.nocombro.mobile.experiments.internal.data

import java.util.UUID
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.nocombro.mobile.sync.mobileUpdatedAt
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperiment
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.toModel

/**
 * Даёт список мобильных экспериментов и меняет состояние их жизненного цикла.
 *
 * Активные и архивные строки наблюдаются раздельно. Удаление охватывает всё дерево
 * эксперимента, чтобы каждый дочерний объект получил tombstone до синхронизации.
 *
 * @param db база мобильного приложения и граница транзакции удаления.
 */
class ExperimentsListRepository(
    private val db: NocombroMobileDatabase,
) {
    private val experimentDao = db.experimentDao
    private val entryDao = db.experimentEntryDao
    private val reminderDao = db.experimentReminderDao
    private val fileDao = db.experimentEntryFileDao

    fun observeActiveExperiments(): Flow<List<MobileExperiment>> {
        return experimentDao.observeExperiments(isArchived = false)
            .map { list -> list.map(MobileExperimentEntity::toModel) }
    }

    fun observeArchivedExperiments(): Flow<List<MobileExperiment>> {
        return experimentDao.observeExperiments(isArchived = true)
            .map { list -> list.map(MobileExperimentEntity::toModel) }
    }

    /**
     * Создаёт пустой эксперимент с новым `syncId` и текущей UTC-версией.
     *
     * @return созданная модель с локальным Room идентификатором либо ошибка записи.
     */
    suspend fun createAndReturnExperiment(): Result<MobileExperiment> {
        return runCatching {
            val experiment = MobileExperimentEntity(
                title = "",
                syncId = UUID.randomUUID().toString(),
                updatedAt = mobileUpdatedAt(),
            )
            val id = experimentDao.create(experiment).toInt()
            experiment.copy(id = id).toModel()
        }
    }

    /**
     * Помечает tombstone эксперимент и все его записи, напоминания и файлы.
     *
     * Все строки получают одну версию, которая строго новее любой версии в дереве.
     * Дочерние tombstone записываются раньше родителя в одной транзакции, поэтому
     * удаление можно безопасно передать на другое устройство без потери потомков.
     *
     * @param id локальный идентификатор эксперимента.
     * @return успех либо ошибка чтения или записи любой строки дерева.
     */
    suspend fun deleteExperiment(id: Int): Result<Unit> {
        return runCatching {
            db.withTransaction {
                val experiment = requireExperiment(id)
                val entries = entryDao.getEntriesByExperiment(id)
                val reminders = reminderDao.getRemindersByExperiment(id)
                val files = entries.map { it.id }.takeIf(List<Int>::isNotEmpty)
                    ?.let { fileDao.getFilesByEntries(it) }
                    .orEmpty()
                val previousVersion = buildList {
                    add(experiment.updatedAt)
                    experiment.deletedAt?.let(::add)
                    entries.forEach { add(it.deletedAt?.takeIf { deleted -> deleted > it.updatedAt } ?: it.updatedAt) }
                    reminders.forEach { add(it.deletedAt?.takeIf { deleted -> deleted > it.updatedAt } ?: it.updatedAt) }
                    files.forEach { add(it.deletedAt?.takeIf { deleted -> deleted > it.updatedAt } ?: it.updatedAt) }
                }.maxOrNull()
                val deletedAt = mobileUpdatedAt(previousVersion)

                files.forEach { fileDao.upsert(it.copy(updatedAt = deletedAt, deletedAt = deletedAt)) }
                reminders.forEach {
                    reminderDao.upsert(it.copy(updatedAt = deletedAt, deletedAt = deletedAt))
                }
                entries.forEach { entryDao.upsert(it.copy(updatedAt = deletedAt, deletedAt = deletedAt)) }
                experimentDao.upsert(
                    experiment.copy(
                        updatedAt = deletedAt,
                        deletedAt = deletedAt,
                    )
                )
            }
        }
    }

    private suspend fun requireExperiment(id: Int): MobileExperimentEntity {
        return requireNotNull(experimentDao.getExperiment(id)) {
            "Experiment $id not found"
        }
    }
}

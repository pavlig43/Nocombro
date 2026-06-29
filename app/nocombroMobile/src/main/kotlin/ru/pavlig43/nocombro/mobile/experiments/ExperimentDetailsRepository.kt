package ru.pavlig43.nocombro.mobile.experiments

import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentReminderEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.experiments.data.toModel

/**
 * Локальный репозиторий деталей mobile-эксперимента.
 */
class ExperimentDetailsRepository(
    private val db: MobileExperimentsDatabase,
) {
    private val experimentDao = db.experimentDao
    private val entryDao = db.experimentEntryDao
    private val reminderDao = db.experimentReminderDao

    /**
     * Следит за живым экспериментом по id.
     */
    fun observeExperiment(id: Int): Flow<MobileExperiment?> {
        return experimentDao.observeExperiment(id)
            .map { experiment -> experiment?.toModel() }
    }

    /**
     * Следит за записями эксперимента.
     */
    fun observeEntries(experimentId: Int): Flow<List<MobileExperimentEntry>> {
        return entryDao.observeEntries(experimentId)
            .map { entries -> entries.map(MobileExperimentEntryEntity::toModel) }
    }

    /**
     * Следит за напоминаниями эксперимента.
     */
    fun observeReminders(experimentId: Int): Flow<List<MobileExperimentReminder>> {
        return reminderDao.observeReminders(experimentId)
            .map { reminders -> reminders.map(MobileExperimentReminderEntity::toModel) }
    }

    /**
     * Следит за выбранной записью журнала.
     */
    fun observeEntry(entryId: Int): Flow<MobileExperimentEntry?> {
        return entryDao.observeEntry(entryId)
            .map { entry -> entry?.toModel() }
    }

    /**
     * Обновляет название и идею эксперимента.
     */
    suspend fun updateExperimentDraft(
        experimentId: Int,
        title: String,
        ideaDescription: String,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val experiment = requireExperiment(experimentId)
            experimentDao.upsert(
                experiment.copy(
                    title = title,
                    ideaDescription = ideaDescription,
                    updatedAt = getCurrentLocalDateTime(),
                )
            )
        }
    }

    /**
     * Меняет архивный флаг эксперимента.
     */
    suspend fun setExperimentArchived(
        experimentId: Int,
        isArchived: Boolean,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val experiment = requireExperiment(experimentId)
            experimentDao.upsert(
                experiment.copy(
                    isArchived = isArchived,
                    updatedAt = getCurrentLocalDateTime(),
                )
            )
        }
    }

    /**
     * Возвращает запись за дату или создаёт её.
     */
    suspend fun getOrCreateEntry(
        experimentId: Int,
        entryDate: LocalDate,
    ): Result<MobileExperimentEntry> = runCatching {
        db.withTransaction {
            entryDao.getEntryByExperimentAndDate(experimentId, entryDate)
                ?.toModel()
                ?: run {
                    val now = getCurrentLocalDateTime()
                    val entry = MobileExperimentEntryEntity(
                        experimentId = experimentId,
                        entryDate = entryDate,
                        syncId = UUID.randomUUID().toString(),
                        updatedAt = now,
                    )
                    val id = entryDao.create(entry).toInt()
                    touchExperiment(experimentId, now)
                    entry.copy(id = id).toModel()
                }
        }
    }

    /**
     * Обновляет текст выбранной записи.
     */
    suspend fun updateEntryContent(
        entryId: Int,
        content: String,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val entry = requireNotNull(entryDao.getEntry(entryId)) {
                "Entry $entryId not found"
            }
            val now = getCurrentLocalDateTime()
            entryDao.upsert(
                entry.copy(
                    content = content,
                    updatedAt = now,
                )
            )
            touchExperiment(entry.experimentId, now)
        }
    }

    /**
     * Создаёт локальное напоминание.
     */
    suspend fun createReminder(
        experimentId: Int,
        text: String,
        reminderDateTime: LocalDateTime,
    ): Result<MobileExperimentReminder> = runCatching {
        db.withTransaction {
            val now = getCurrentLocalDateTime()
            val reminder = MobileExperimentReminderEntity(
                experimentId = experimentId,
                text = text,
                reminderDateTime = reminderDateTime,
                syncId = UUID.randomUUID().toString(),
                updatedAt = now,
            )
            val id = reminderDao.create(reminder).toInt()
            touchExperiment(experimentId, now)
            reminder.copy(id = id).toModel()
        }
    }

    /**
     * Меняет локальное напоминание.
     */
    suspend fun updateReminder(
        reminderId: Int,
        text: String,
        reminderDateTime: LocalDateTime,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val reminder = requireNotNull(reminderDao.getReminder(reminderId)) {
                "Reminder $reminderId not found"
            }
            val now = getCurrentLocalDateTime()
            reminderDao.upsert(
                reminder.copy(
                    text = text,
                    reminderDateTime = reminderDateTime,
                    updatedAt = now,
                )
            )
            touchExperiment(reminder.experimentId, now)
        }
    }

    /**
     * Помечает напоминание удалённым.
     */
    suspend fun deleteReminder(reminderId: Int): Result<Unit> = runCatching {
        db.withTransaction {
            val reminder = requireNotNull(reminderDao.getReminder(reminderId)) {
                "Reminder $reminderId not found"
            }
            val now = getCurrentLocalDateTime()
            reminderDao.upsert(
                reminder.copy(
                    updatedAt = now,
                    deletedAt = now,
                )
            )
            touchExperiment(reminder.experimentId, now)
        }
    }

    private suspend fun requireExperiment(id: Int): MobileExperimentEntity {
        return requireNotNull(experimentDao.getExperiment(id)) {
            "Experiment $id not found"
        }
    }

    private suspend fun touchExperiment(
        experimentId: Int,
        updatedAt: LocalDateTime,
    ) {
        val experiment = requireExperiment(experimentId)
        experimentDao.upsert(experiment.copy(updatedAt = updatedAt))
    }
}

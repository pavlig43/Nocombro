package ru.pavlig43.experiments.internal.data

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase

/**
 * Проверяет общую версию каскадного tombstone и независимость версии родителя.
 */
class ExperimentsRepositoryTest : DesktopMainDispatcherFunSpec({

    test("delete tombstones files reminders entries and parent with one newer version") {
        withEmptyTestDatabase { db ->
            val oldVersion = LocalDateTime(2099, 1, 1, 0, 0)
            val experiment = Experiment(title = "E", updatedAt = oldVersion)
            val experimentId = db.experimentDao.create(experiment).toInt()
            val entry = ExperimentEntry(
                experimentId = experimentId,
                entryDate = LocalDate(2099, 1, 1),
                updatedAt = oldVersion,
            )
            val entryId = db.experimentEntryDao.create(entry).toInt()
            val reminder = ExperimentReminder(
                experimentId = experimentId,
                text = "R",
                reminderDateTime = oldVersion,
                updatedAt = oldVersion,
            )
            db.experimentReminderDao.create(reminder)
            val file = FileBD(
                ownerId = entryId,
                ownerFileType = OwnerType.EXPERIMENT_ENTRY,
                displayName = "f",
                path = "f",
                updatedAt = oldVersion,
            )
            db.fileDao.upsertFiles(listOf(file))

            ExperimentsRepository(db).deleteExperiment(experimentId).getOrThrow()

            val parentDeletedAt = db.experimentDao.getExperiment(experimentId)!!.deletedAt!!
            val entryDeletedAt = db.experimentEntryDao.getEntry(entryId)!!.deletedAt
            val reminderDeletedAt = db.experimentReminderDao.getAll().single().deletedAt
            val fileDeletedAt = db.fileDao.getFileBySyncId(file.syncId)!!.deletedAt
            parentDeletedAt shouldBeGreaterThan oldVersion
            entryDeletedAt shouldBe parentDeletedAt
            reminderDeletedAt shouldBe parentDeletedAt
            fileDeletedAt shouldBe parentDeletedAt
        }
    }

    test("child create does not change the parent sync version") {
        withEmptyTestDatabase { db ->
            val parentVersion = LocalDateTime(2026, 1, 1, 0, 0)
            val experimentId = db.experimentDao.create(
                Experiment(title = "E", updatedAt = parentVersion)
            ).toInt()

            ExperimentsRepository(db)
                .createEntryForDate(experimentId, LocalDate(2026, 1, 2))
                .getOrThrow()

            db.experimentDao.getExperiment(experimentId)!!.updatedAt shouldBe parentVersion
        }
    }
})

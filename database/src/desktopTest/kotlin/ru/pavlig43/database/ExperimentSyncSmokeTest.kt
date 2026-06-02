package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.sync.FileSyncPayload
import ru.pavlig43.database.data.sync.RemotePushChange
import ru.pavlig43.database.data.sync.SyncChangeType
import ru.pavlig43.database.data.sync.SyncEntityExportRepository
import ru.pavlig43.database.data.sync.SyncPayloadEnvelope
import ru.pavlig43.database.data.sync.SyncPushChange
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import ru.pavlig43.testkit.scenario

class ExperimentSyncSmokeTest : DesktopMainDispatcherFunSpec({

    test(
        scenario(
            given = "an empty database",
            whenAction = "an experiment entry is created for one date",
            thenResult = "the same date resolves to the same row and a duplicate is not created",
        )
    ) {
        withEmptyTestDatabase { db ->
            val experiment = Experiment(title = "Гипотеза")
            val experimentId = db.experimentDao.create(experiment).toInt()

            val entry = ExperimentEntry(
                experimentId = experimentId,
                entryDate = LocalDate(2026, 6, 2),
                content = "Первый подход",
            )
            val entryId = db.experimentEntryDao.create(entry).toInt()
            val existing = db.experimentEntryDao.getEntryByExperimentAndDate(
                experimentId = experimentId,
                entryDate = LocalDate(2026, 6, 2),
            )

            existing shouldNotBe null
            existing?.id shouldBe entryId
        }
    }

    test(
        scenario(
            given = "an experiment entry with a file attachment",
            whenAction = "the file sync payload is exported",
            thenResult = "the payload points to the experiment entry sync id",
        )
    ) {
        withEmptyTestDatabase { db ->
            val experiment = Experiment(title = "Экспорт")
            val experimentId = db.experimentDao.create(experiment).toInt()
            val savedExperiment = experiment.copy(id = experimentId)

            val entry = ExperimentEntry(
                experimentId = experimentId,
                entryDate = LocalDate(2026, 6, 2),
                content = "Запись",
            )
            val entryId = db.experimentEntryDao.create(entry).toInt()
            val savedEntry = entry.copy(id = entryId)

            val file = FileBD(
                ownerId = entryId,
                ownerFileType = OwnerType.EXPERIMENT_ENTRY,
                displayName = "note.txt",
                path = "C:/tmp/note.txt",
            )
            db.fileDao.upsertFiles(listOf(file))
            val savedFile = db.fileDao.getFileByOwnerAndDisplayName(
                ownerId = entryId,
                ownerFileType = OwnerType.EXPERIMENT_ENTRY,
                displayName = "note.txt",
            )!!

            val exported = SyncEntityExportRepository(db).export(
                SyncPushChange(
                    entityTable = ru.pavlig43.database.data.files.FILE_TABLE_NAME,
                    entityLocalId = savedFile.syncId,
                    changeType = SyncChangeType.UPSERT,
                    sourceQueueIds = listOf(1L),
                    lastQueuedAt = defaultUpdatedAt(),
                )
            )

            val payload = Json.decodeFromString<SyncPayloadEnvelope<FileSyncPayload>>(
                exported.payloadJson!!
            ).payload

            payload.ownerType shouldBe OwnerType.EXPERIMENT_ENTRY
            payload.ownerSyncId shouldBe savedEntry.syncId
            savedExperiment.syncId shouldNotBe ""
        }
    }
})

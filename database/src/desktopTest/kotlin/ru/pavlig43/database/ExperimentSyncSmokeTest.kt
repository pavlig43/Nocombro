package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.database.data.sync.ExperimentReminderSyncPayload
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.sync.RemotePullChange
import ru.pavlig43.database.data.sync.FileSyncPayload
import ru.pavlig43.database.data.sync.RemotePushChange
import ru.pavlig43.database.data.sync.SyncChangeType
import ru.pavlig43.database.data.sync.SyncEntityExportRepository
import ru.pavlig43.database.data.sync.SyncPayloadEnvelope
import ru.pavlig43.database.data.sync.SyncPushChange
import ru.pavlig43.database.data.sync.SyncRemoteApplyRepository
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

    test(
        scenario(
            given = "an experiment reminder stored locally",
            whenAction = "its sync payload is exported",
            thenResult = "the payload points to the owning experiment sync id",
        )
    ) {
        withEmptyTestDatabase { db ->
            val experiment = Experiment(title = "Напоминание")
            val experimentId = db.experimentDao.create(experiment).toInt()
            val savedExperiment = experiment.copy(id = experimentId)

            val reminder = ExperimentReminder(
                experimentId = experimentId,
                text = "Проверить результат",
                reminderDateTime = LocalDateTime(2026, 6, 3, 9, 30),
            )
            val reminderId = db.experimentReminderDao.create(reminder).toInt()
            val savedReminder = reminder.copy(id = reminderId)

            val exported = SyncEntityExportRepository(db).export(
                SyncPushChange(
                    entityTable = EXPERIMENT_REMINDER_TABLE_NAME,
                    entityLocalId = savedReminder.syncId,
                    changeType = SyncChangeType.UPSERT,
                    sourceQueueIds = listOf(1L),
                    lastQueuedAt = defaultUpdatedAt(),
                )
            )

            val payload = Json.decodeFromString<SyncPayloadEnvelope<ExperimentReminderSyncPayload>>(
                exported.payloadJson!!
            ).payload

            payload.experimentSyncId shouldBe savedExperiment.syncId
            payload.text shouldBe "Проверить результат"
            exported.experimentReminderEmailSource?.experimentSyncId shouldBe savedExperiment.syncId
            exported.experimentReminderEmailSource?.experimentTitle shouldBe "Напоминание"
            exported.transactionReminderEmailSource shouldBe null
        }
    }

    test(
        scenario(
            given = "a transaction reminder stored locally",
            whenAction = "its sync payload is exported",
            thenResult = "only the transaction email source contract is populated",
        )
    ) {
        withEmptyTestDatabase { db ->
            val transaction = Transact(
                transactionType = TransactionType.BUY,
                createdAt = LocalDateTime(2026, 6, 3, 7, 45),
                comment = "Сырье",
                isCompleted = false,
            )
            val transactionId = db.transactionDao.create(transaction).toInt()
            val savedTransaction = transaction.copy(id = transactionId)
            val reminder = ReminderBD(
                transactionId = transactionId,
                text = "Связаться с поставщиком",
                reminderDateTime = LocalDateTime(2026, 6, 3, 9, 15),
            )
            db.reminderDao.upsertAll(listOf(reminder))
            val savedReminder = db.reminderDao.getByTransactionId(transactionId).single()

            val exported = SyncEntityExportRepository(db).export(
                SyncPushChange(
                    entityTable = ru.pavlig43.database.data.transact.reminder.REMINDER_TABLE_NAME,
                    entityLocalId = savedReminder.syncId,
                    changeType = SyncChangeType.UPSERT,
                    sourceQueueIds = listOf(1L),
                    lastQueuedAt = defaultUpdatedAt(),
                )
            )

            exported.transactionReminderEmailSource?.transactionSyncId shouldBe savedTransaction.syncId
            exported.transactionReminderEmailSource?.transactionType shouldBe TransactionType.BUY.displayName
            exported.experimentReminderEmailSource shouldBe null
        }
    }

    test(
        scenario(
            given = "an incoming remote experiment reminder",
            whenAction = "it is applied and later soft-deleted",
            thenResult = "the reminder is restored by sync id and then hidden from active lists",
        )
    ) {
        withEmptyTestDatabase { db ->
            val experiment = Experiment(title = "Remote")
            val experimentId = db.experimentDao.create(experiment).toInt()
            val savedExperiment = experiment.copy(id = experimentId)
            val repository = SyncRemoteApplyRepository(db)
            val payload = SyncPayloadEnvelope(
                payload = ExperimentReminderSyncPayload(
                    syncId = "exp-rem-1",
                    experimentSyncId = savedExperiment.syncId,
                    text = "Удалить потом",
                    reminderDateTime = LocalDateTime(2026, 6, 3, 8, 0),
                    updatedAt = LocalDateTime(2026, 6, 3, 8, 0),
                )
            )

            repository.applyChanges(
                listOf(
                    RemotePullChange(
                        cursor = "1",
                        sourceDeviceId = "device-a",
                        entityTable = EXPERIMENT_REMINDER_TABLE_NAME,
                        entitySyncId = "exp-rem-1",
                        changeType = SyncChangeType.UPSERT,
                        changedAt = LocalDateTime(2026, 6, 3, 8, 0),
                        payloadJson = Json.encodeToString(payload),
                    )
                )
            )

            db.experimentReminderDao.observeReminders(experimentId).first().size shouldBe 1

            repository.applyChanges(
                listOf(
                    RemotePullChange(
                        cursor = "2",
                        sourceDeviceId = "device-a",
                        entityTable = EXPERIMENT_REMINDER_TABLE_NAME,
                        entitySyncId = "exp-rem-1",
                        changeType = SyncChangeType.DELETE,
                        changedAt = LocalDateTime(2026, 6, 3, 9, 0),
                        payloadJson = null,
                    )
                )
            )

            db.experimentReminderDao.observeReminders(experimentId).first().size shouldBe 0
            db.experimentReminderDao.getReminderBySyncId("exp-rem-1")?.deletedAt shouldNotBe null
        }
    }

    test(
        scenario(
            given = "active and deleted experiment reminders",
            whenAction = "today notifications are observed",
            thenResult = "only active overdue/today reminders are returned",
        )
    ) {
        withEmptyTestDatabase { db ->
            val experiment = Experiment(title = "Уведомления")
            val experimentId = db.experimentDao.create(experiment).toInt()

            db.experimentReminderDao.create(
                ExperimentReminder(
                    experimentId = experimentId,
                    text = "Активное",
                    reminderDateTime = LocalDateTime(2026, 6, 3, 10, 0),
                )
            )
            db.experimentReminderDao.upsert(
                ExperimentReminder(
                    experimentId = experimentId,
                    text = "Удаленное",
                    reminderDateTime = LocalDateTime(2026, 6, 3, 11, 0),
                    syncId = "deleted-reminder",
                    deletedAt = LocalDateTime(2026, 6, 3, 11, 30),
                )
            )

            val notifications = db.experimentReminderDao.observeTodayReminders().first()

            notifications.size shouldBe 1
            notifications.single().displayName shouldBe "Уведомления: Активное"
        }
    }
})

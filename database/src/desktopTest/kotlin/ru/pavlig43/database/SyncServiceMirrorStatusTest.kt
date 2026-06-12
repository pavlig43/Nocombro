package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileRef
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteStorageObject
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncStateEntity
import ru.pavlig43.database.data.sync.SyncStateRepository
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPullRequest
import ru.pavlig43.database.data.sync.mirror.MirrorPullResult
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorPushResult
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlanner
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteStatus
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRemoteGateway
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase

class SyncServiceMirrorStatusTest : DesktopMainDispatcherFunSpec({

    test("mirror is the only source of sync status") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val checkedAt = LocalDateTime(2026, 6, 11, 15, 0)
            val service = createSyncService(db, StatusMirrorGateway(checkedAt))

            val status = service.getStatus()

            status.remoteSyncConfigured shouldBe true
            status.hasRemoteChanges shouldBe true
            status.pendingLocalChangesCount shouldBe 0
            status.remoteChangesCount shouldBe 1
            status.lastStatusCheckAt shouldBe checkedAt
            status.remoteError shouldBe null
        }
    }

    test("syncOnce returns the file download summary produced after mirror pull") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val service = createSyncService(
                db = db,
                gateway = StatusMirrorGateway(LocalDateTime(2026, 6, 11, 15, 0)),
                fileRepository = RemoteFileBatchDownloadRepository(
                    db = db,
                    remoteFileStorageGateway = ConfiguredFileStorageGateway(),
                ),
            )

            val result = service.syncOnce()

            result.error shouldBe null
            result.filesDownloadSummary?.scannedCount shouldBe 0
        }
    }
})

private fun createSyncService(
    db: NocombroDatabase,
    gateway: MirrorSyncRemoteGateway,
    fileRepository: RemoteFileBatchDownloadRepository? = null,
): SyncService {
    val applyRepository = MirrorLocalApplyRepository(
        db = db,
        entityApplyRepository = MirrorEntityApplyRepository(db),
    )
    return SyncService(
        syncStateRepository = SyncStateRepository(db.syncStateDao),
        mirrorReconciliationService = MirrorReconciliationService(
            localSnapshotRepository = MirrorLocalSnapshotRepository(db),
            remoteGateway = gateway,
            planner = MirrorReconciliationPlanner(),
            localApplyRepository = applyRepository,
        ),
        remoteFileBatchDownloadRepository = fileRepository,
    )
}

private class ConfiguredFileStorageGateway : RemoteFileStorageGateway {
    override val providerId = "fake"
    override fun isConfigured() = true
    override suspend fun upload(objectKey: String, localPath: String) =
        Result.success(RemoteFileRef(providerId, objectKey))
    override suspend fun download(objectKey: String, localPath: String) = Result.success(Unit)
    override suspend fun listObjects() = Result.success(emptyList<RemoteStorageObject>())
    override suspend fun delete(objectKey: String) = Result.success(Unit)
}

private class StatusMirrorGateway(
    private val checkedAt: LocalDateTime,
) : MirrorSyncRemoteGateway {
    private val remoteRow = VendorMirrorRow(
        syncId = "remote-vendor",
        displayName = "Remote vendor",
        comment = "",
        updatedAt = checkedAt,
    )

    override suspend fun getStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = MirrorSyncTable.mirroredBusinessTables
            .mapTo(mutableSetOf(), MirrorSyncTable::tableName),
        checkedAt = checkedAt,
    )

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>) =
        Result.success(
            MirrorRemoteSnapshot(
                loadedAt = checkedAt,
                rowsByTable = tables.associateWith { table ->
                    if (table == MirrorSyncTable.VENDOR) listOf(remoteRow) else emptyList()
                },
            )
        )

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>) =
        Result.success(MirrorPushResult(checkedAt, changes.mapTo(mutableSetOf()) { it.table }))

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.success(
            MirrorPullResult(
                pulledAt = checkedAt,
                changes = request.tables.flatMap { table ->
                    if (table == MirrorSyncTable.VENDOR) {
                        listOf(MirrorPushEntityChange(table, remoteRow))
                    } else {
                        emptyList()
                    }
                },
            )
        )
}

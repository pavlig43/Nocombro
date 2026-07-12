package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileRef
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteStorageObject
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncAnalysisReportWriter
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
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import java.nio.file.Files

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
            val gateway = StatusMirrorGateway(LocalDateTime(2026, 6, 11, 15, 0))
            val service = createSyncService(
                db = db,
                gateway = gateway,
                fileRepository = RemoteFileBatchDownloadRepository(
                    db = db,
                    remoteFileStorageGateway = ConfiguredFileStorageGateway(),
                ),
            )

            val result = service.syncOnce()

            result.error shouldBe null
            result.filesDownloadSummary?.scannedCount shouldBe 0
            gateway.configurationStatusCalls shouldBe 1
            gateway.statusCalls shouldBe 0
            gateway.snapshotCalls shouldBe 1
            result.status.pendingLocalChangesCount shouldBe 0
            result.status.remoteChangesCount shouldBe 0
        }
    }

    test("sync analysis report does not write mirror state timestamps or files") {
        withEmptyTestDatabase { db ->
            val initialState = SyncStateEntity(
                lastPushAt = LocalDateTime(2026, 6, 10, 10, 0),
                lastPullAt = LocalDateTime(2026, 6, 10, 11, 0),
            )
            db.syncStateDao.upsertSyncState(initialState)
            val gateway = StatusMirrorGateway(LocalDateTime(2026, 6, 11, 15, 0))
            val fileGateway = ConfiguredFileStorageGateway()
            val service = createSyncService(
                db = db,
                gateway = gateway,
                fileRepository = RemoteFileBatchDownloadRepository(db, fileGateway),
                reportWriter = SyncAnalysisReportWriter(
                    reportDirectory = { Files.createTempDirectory("sync-report-service").toFile() },
                ),
            )

            val result = service.createSyncAnalysisReport()

            result.isSuccess shouldBe true
            gateway.pushCalls shouldBe 0
            gateway.pullCalls shouldBe 0
            fileGateway.downloadCalls shouldBe 0
            db.syncStateDao.getSyncState() shouldBe initialState
        }
    }

    test("fresh Room restores 150 remote rows from one remote snapshot") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val gateway = StatusMirrorGateway(
                checkedAt = LocalDateTime(2026, 6, 11, 15, 0),
                remoteRowCount = 150,
            )

            val result = createSyncService(db, gateway).syncOnce()

            result.error shouldBe null
            db.vendorDao.getAll().size shouldBe 150
            gateway.configurationStatusCalls shouldBe 1
            gateway.statusCalls shouldBe 0
            gateway.snapshotCalls shouldBe 1
            result.status.pendingLocalChangesCount shouldBe 0
            result.status.remoteChangesCount shouldBe 0
        }
    }

    test("local row created while remote snapshot loads remains pending for push") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val checkedAt = LocalDateTime(2026, 6, 11, 15, 0)
            val gateway = StatusMirrorGateway(
                checkedAt = checkedAt,
                remoteRowCount = 0,
                onSnapshotLoad = {
                    db.vendorDao.create(
                        Vendor(
                            displayName = "Concurrent vendor",
                            updatedAt = LocalDateTime(2026, 6, 11, 15, 1),
                        )
                    )
                },
            )

            val result = createSyncService(db, gateway).syncOnce()

            result.error shouldBe null
            result.status.pendingLocalChangesCount shouldBe 1
            result.status.remoteChangesCount shouldBe 0
            gateway.snapshotCalls shouldBe 1
            gateway.pushCalls shouldBe 0
        }
    }

    test("remote snapshot failure preserves configured YDB status and error") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val gateway = StatusMirrorGateway(
                checkedAt = LocalDateTime(2026, 6, 11, 15, 0),
                snapshotError = "snapshot unavailable",
            )

            val result = createSyncService(db, gateway).syncOnce()

            result.error shouldBe "Mirror remote snapshot failed: snapshot unavailable"
            result.status.remoteSyncConfigured shouldBe true
            result.status.remoteError shouldBe result.error
            gateway.snapshotCalls shouldBe 1
        }
    }

    test("missing YDB configuration remains unconfigured after sync failure") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val gateway = StatusMirrorGateway(
                checkedAt = LocalDateTime(2026, 6, 11, 15, 0),
                configured = false,
            )

            val result = createSyncService(db, gateway).syncOnce()

            result.error shouldBe "Mirror sync is not configured"
            result.status.remoteSyncConfigured shouldBe false
            result.status.remoteError shouldBe result.error
            gateway.snapshotCalls shouldBe 0
        }
    }
})

private fun createSyncService(
    db: NocombroDatabase,
    gateway: MirrorSyncRemoteGateway,
    fileRepository: RemoteFileBatchDownloadRepository? = null,
    reportWriter: SyncAnalysisReportWriter = SyncAnalysisReportWriter(),
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
        syncAnalysisReportWriter = reportWriter,
    )
}

private class ConfiguredFileStorageGateway : RemoteFileStorageGateway {
    var downloadCalls = 0
    override val providerId = "fake"
    override fun isConfigured() = true
    override suspend fun upload(objectKey: String, localPath: String) =
        Result.success(RemoteFileRef(providerId, objectKey))
    override suspend fun download(objectKey: String, localPath: String): Result<Unit> {
        downloadCalls++
        return Result.success(Unit)
    }
    override suspend fun listObjects() = Result.success(emptyList<RemoteStorageObject>())
    override suspend fun delete(objectKey: String) = Result.success(Unit)
}

private class StatusMirrorGateway(
    private val checkedAt: LocalDateTime,
    remoteRowCount: Int = 1,
    private val configured: Boolean = true,
    private val snapshotError: String? = null,
    private val onSnapshotLoad: suspend () -> Unit = {},
) : MirrorSyncRemoteGateway {
    var configurationStatusCalls = 0
    var statusCalls = 0
    var snapshotCalls = 0
    var pushCalls = 0
    var pullCalls = 0
    private val remoteRows = List(remoteRowCount) { index ->
        VendorMirrorRow(
            syncId = "remote-vendor-$index",
            displayName = "Remote vendor $index",
            comment = "",
            updatedAt = checkedAt,
        )
    }

    override suspend fun getConfigurationStatus(): MirrorRemoteStatus {
        configurationStatusCalls++
        return remoteStatus()
    }

    override suspend fun getStatus(): MirrorRemoteStatus {
        statusCalls++
        return remoteStatus()
    }

    private fun remoteStatus() = MirrorRemoteStatus(
        configured = configured,
        availableTables = if (configured) {
            MirrorSyncTable.mirroredBusinessTables.mapTo(mutableSetOf(), MirrorSyncTable::tableName)
        } else {
            emptySet()
        },
        checkedAt = checkedAt,
    )

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>): Result<MirrorRemoteSnapshot> {
        snapshotCalls++
        snapshotError?.let { return Result.failure(IllegalStateException(it)) }
        onSnapshotLoad()
        return Result.success(
            MirrorRemoteSnapshot(
                loadedAt = checkedAt,
                rowsByTable = tables.associateWith { table ->
                    if (table == MirrorSyncTable.VENDOR) remoteRows else emptyList()
                },
            )
        )
    }

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>): Result<MirrorPushResult> {
        pushCalls++
        return Result.success(MirrorPushResult(checkedAt, changes.mapTo(mutableSetOf()) { it.table }))
    }

    override suspend fun pullMirrorState(request: MirrorPullRequest): Result<MirrorPullResult> {
        pullCalls++
        return Result.success(
            MirrorPullResult(
                pulledAt = checkedAt,
                changes = request.tables.flatMap { table ->
                    if (table == MirrorSyncTable.VENDOR) {
                        remoteRows.map { MirrorPushEntityChange(table, it) }
                    } else {
                        emptyList()
                    }
                },
            )
        )
    }
}

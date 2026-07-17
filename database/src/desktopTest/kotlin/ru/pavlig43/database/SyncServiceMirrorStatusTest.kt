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
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorConflictWinner
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPullRequest
import ru.pavlig43.database.data.sync.mirror.MirrorPullResult
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorPushResult
import ru.pavlig43.database.data.sync.mirror.MirrorPushRejection
import ru.pavlig43.database.data.sync.mirror.MirrorPushRejectionReason
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlanner
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteStatus
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRemoteGateway
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import java.nio.file.Files

/**
 * Проверяет статус, условный push и ручное разрешение конфликтов SyncService.
 *
 * Сценарии моделируют конкурентные правки между чтением снимка и записью: один
 * отказ должен привести к повторной сверке, второй — к ошибке без третьей попытки.
 */
class SyncServiceMirrorStatusTest : DesktopMainDispatcherFunSpec({

    test("mirror is the only source of sync status") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val checkedAt = LocalDateTime(2026, 6, 11, 15, 0)
            val gateway = StatusMirrorGateway(checkedAt)
            val service = createSyncService(db, gateway)

            val status = service.getStatus()

            status.remoteSyncConfigured shouldBe true
            status.hasRemoteChanges shouldBe true
            status.pendingLocalChangesCount shouldBe 0
            status.remoteChangesCount shouldBe 1
            status.lastStatusCheckAt shouldBe checkedAt
            status.remoteError shouldBe null
            service.status.value shouldBe status
            gateway.configurationStatusCalls shouldBe 1
            gateway.statusCalls shouldBe 0
            gateway.snapshotCalls shouldBe 1
        }
    }

    test("status snapshot failure does not run a separate table probe") {
        withEmptyTestDatabase { db ->
            db.syncStateDao.upsertSyncState(SyncStateEntity())
            val gateway = StatusMirrorGateway(
                checkedAt = LocalDateTime(2026, 6, 11, 15, 0),
                snapshotError = "snapshot unavailable",
            )
            val service = createSyncService(db, gateway)

            val status = service.getStatus()

            status.remoteSyncConfigured shouldBe true
            status.remoteError shouldBe "snapshot unavailable"
            gateway.configurationStatusCalls shouldBe 1
            gateway.statusCalls shouldBe 0
            gateway.snapshotCalls shouldBe 1
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

    test("rejected stale push reloads remote once and applies the newer row") {
        withEmptyTestDatabase { db ->
            val local = Vendor(
                displayName = "Local",
                updatedAt = LocalDateTime(2026, 1, 1, 0, 0),
            )
            db.vendorDao.create(local)
            val gateway = RejectingStalePushGateway(
                remoteVersion = LocalDateTime(2026, 1, 2, 0, 0),
            )

            val result = createSyncService(db, gateway).syncOnce()

            result.error shouldBe null
            gateway.snapshotCalls shouldBe 2
            gateway.pushCalls shouldBe 1
            result.status.pendingLocalChangesCount shouldBe 0
            db.vendorDao.getVendorBySyncId(local.syncId)!!.displayName shouldBe "Remote"
            result.status.conflicts shouldBe emptyList()
        }
    }

    test("rejected push retries once when a concurrent local edit is newer") {
        withEmptyTestDatabase { db ->
            val firstLocalVersion = LocalDateTime(2026, 1, 1, 0, 0)
            val remoteVersion = LocalDateTime(2026, 1, 2, 0, 0)
            val concurrentLocalVersion = LocalDateTime(2026, 1, 3, 0, 0)
            val local = Vendor(displayName = "Local", updatedAt = firstLocalVersion)
            db.vendorDao.create(local)
            val gateway = ConcurrentRetryGateway(
                firstRemoteVersion = remoteVersion,
                onFirstRejection = {
                    val current = requireNotNull(db.vendorDao.getVendorBySyncId(local.syncId))
                    db.vendorDao.updateVendor(
                        current.copy(
                            displayName = "Concurrent local",
                            updatedAt = concurrentLocalVersion,
                        )
                    )
                },
            )

            val result = createSyncService(db, gateway).syncOnce()

            result.error shouldBe null
            gateway.snapshotCalls shouldBe 2
            gateway.pushCalls shouldBe 2
            gateway.remoteRow?.displayName shouldBe "Concurrent local"
            result.status.pendingLocalChangesCount shouldBe 0
        }
    }

    test("second rejected push fails without a third attempt") {
        withEmptyTestDatabase { db ->
            val local = Vendor(
                displayName = "Local",
                updatedAt = LocalDateTime(2026, 1, 1, 0, 0),
            )
            db.vendorDao.create(local)
            val gateway = ConcurrentRetryGateway(
                firstRemoteVersion = LocalDateTime(2026, 1, 2, 0, 0),
                secondRemoteVersion = LocalDateTime(2026, 1, 4, 0, 0),
                onFirstRejection = {
                    val current = requireNotNull(db.vendorDao.getVendorBySyncId(local.syncId))
                    db.vendorDao.updateVendor(
                        current.copy(
                            displayName = "Concurrent local",
                            updatedAt = LocalDateTime(2026, 1, 3, 0, 0),
                        )
                    )
                },
            )

            val result = createSyncService(db, gateway).syncOnce()

            result.error?.contains("after one remote refresh") shouldBe true
            result.error?.contains("table=vendor, sync_id=${local.syncId}") shouldBe true
            gateway.snapshotCalls shouldBe 2
            gateway.pushCalls shouldBe 2
        }
    }

    test("equal-version rejection becomes a conflict after one refresh") {
        withEmptyTestDatabase { db ->
            val version = LocalDateTime(2026, 1, 1, 0, 0)
            val local = Vendor(displayName = "Local", updatedAt = version)
            db.vendorDao.create(local)
            val gateway = ConcurrentRetryGateway(firstRemoteVersion = version)

            val result = createSyncService(db, gateway).syncOnce()

            result.error shouldBe null
            result.status.conflicts.size shouldBe 1
            result.status.conflicts.single().localRow.syncId shouldBe local.syncId
            gateway.snapshotCalls shouldBe 2
            gateway.pushCalls shouldBe 1
        }
    }

    test("conflict resolution persists either selected payload with a newer version") {
        listOf(
            MirrorConflictWinner.LOCAL to "Local",
            MirrorConflictWinner.REMOTE to "Remote",
        ).forEach { (winner, expectedName) ->
            withEmptyTestDatabase { db ->
                val version = LocalDateTime(2026, 1, 1, 0, 0)
                val local = Vendor(displayName = "Local", updatedAt = version)
                db.vendorDao.create(local)
                val gateway = ConditionalVendorGateway(
                    VendorMirrorRow(
                        syncId = local.syncId,
                        displayName = "Remote",
                        comment = "",
                        updatedAt = version,
                    )
                )
                val service = createSyncService(db, gateway)
                val conflict = service.getStatus().conflicts.single()

                val resolvedStatus = service.resolveConflict(conflict, winner).getOrThrow()

                val resolvedLocal = requireNotNull(db.vendorDao.getVendorBySyncId(local.syncId))
                resolvedLocal.displayName shouldBe expectedName
                (resolvedLocal.updatedAt > version) shouldBe true
                gateway.remoteRow.displayName shouldBe expectedName
                gateway.remoteRow.updatedAt shouldBe resolvedLocal.updatedAt
                gateway.pushCalls shouldBe 1
                resolvedStatus.conflicts shouldBe emptyList()
            }
        }
    }

    test("rejected conflict resolution keeps Room and returns refreshed rows to Doctor") {
        withEmptyTestDatabase { db ->
            val version = LocalDateTime(2026, 1, 1, 0, 0)
            val local = Vendor(displayName = "Local", updatedAt = version)
            db.vendorDao.create(local)
            val remote = VendorMirrorRow(
                syncId = local.syncId,
                displayName = "Remote",
                comment = "",
                updatedAt = version,
            )
            val gateway = ConflictResolutionRetryGateway(
                initialRemoteRow = remote,
                onFirstRejection = { competing ->
                    val current = requireNotNull(db.vendorDao.getVendorBySyncId(local.syncId))
                    db.vendorDao.updateVendor(
                        current.copy(
                            displayName = "Concurrent local",
                            updatedAt = defaultUpdatedAt(competing.updatedAt),
                        )
                    )
                },
            )
            val conflict = MirrorVersionConflict(
                table = MirrorSyncTable.VENDOR,
                localRow = local.toMirrorRowForTest(),
                remoteRow = remote,
            )

            val service = createSyncService(db, gateway)
            val result = service.resolveConflict(conflict, MirrorConflictWinner.LOCAL)

            result.isFailure shouldBe true
            gateway.pushCalls shouldBe 1
            gateway.snapshotCalls shouldBe 3
            db.vendorDao.getVendorBySyncId(local.syncId)?.displayName shouldBe "Concurrent local"
            gateway.remoteRow.displayName shouldBe "Concurrent remote"
            val refreshedConflict = requireNotNull(service.status.value).conflicts.single()
            (refreshedConflict.localRow as VendorMirrorRow).displayName shouldBe "Concurrent local"
            (refreshedConflict.remoteRow as VendorMirrorRow).displayName shouldBe "Concurrent remote"
        }
    }

    test("conflict resolution does not write when Room changes before conditional apply") {
        withEmptyTestDatabase { db ->
            val version = LocalDateTime(2026, 1, 1, 0, 0)
            val local = Vendor(displayName = "Local", updatedAt = version)
            db.vendorDao.create(local)
            val remote = VendorMirrorRow(
                syncId = local.syncId,
                displayName = "Remote",
                comment = "",
                updatedAt = version,
            )
            val gateway = ConditionalVendorGateway(
                initialRemoteRow = remote,
                onFirstSnapshot = {
                    val current = requireNotNull(db.vendorDao.getVendorBySyncId(local.syncId))
                    db.vendorDao.updateVendor(
                        current.copy(
                            displayName = "Concurrent local",
                            updatedAt = defaultUpdatedAt(current.updatedAt),
                        )
                    )
                },
            )
            val conflict = MirrorVersionConflict(
                table = MirrorSyncTable.VENDOR,
                localRow = local.toMirrorRowForTest(),
                remoteRow = remote,
            )

            val result = createSyncService(db, gateway)
                .resolveConflict(conflict, MirrorConflictWinner.LOCAL)

            result.isFailure shouldBe true
            result.exceptionOrNull()?.message?.contains("Обновите список конфликтов") shouldBe true
            db.vendorDao.getVendorBySyncId(local.syncId)?.displayName shouldBe "Concurrent local"
            gateway.remoteRow.displayName shouldBe "Remote"
            gateway.pushCalls shouldBe 0
            gateway.snapshotCalls shouldBe 2
        }
    }

    test("sync state timestamps upsert the default row atomically") {
        withEmptyTestDatabase { db ->
            val repository = SyncStateRepository(db.syncStateDao)
            val pushedAt = LocalDateTime(2026, 1, 1, 0, 0)
            val pulledAt = LocalDateTime(2026, 1, 2, 0, 0)

            repository.updateLastPushAt(pushedAt)
            repository.updateLastPullAt(pulledAt)

            db.syncStateDao.getSyncState() shouldBe SyncStateEntity(
                lastPushAt = pushedAt,
                lastPullAt = pulledAt,
            )
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

/**
 * Считает вызовы дешёвой проверки, полной проверки и загрузки snapshot.
 *
 * Нужен, чтобы статус SyncService не делал лишний probe таблиц перед тем же
 * сетевым чтением и корректно публиковал ошибку snapshot.
 */
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

/** Имитирует первый push, проигравший более новой удалённой строке. */
private class RejectingStalePushGateway(
    private val remoteVersion: LocalDateTime,
) : MirrorSyncRemoteGateway {
    var snapshotCalls = 0
    var pushCalls = 0
    private var remoteRow: VendorMirrorRow? = null

    override suspend fun getConfigurationStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = emptySet(),
        checkedAt = remoteVersion,
    )

    override suspend fun getStatus() = getConfigurationStatus()

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>): Result<MirrorRemoteSnapshot> {
        snapshotCalls++
        return Result.success(
            MirrorRemoteSnapshot(
                loadedAt = remoteVersion,
                rowsByTable = tables.associateWith { table ->
                    if (table == MirrorSyncTable.VENDOR) listOfNotNull(remoteRow) else emptyList()
                },
            )
        )
    }

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>): Result<MirrorPushResult> {
        pushCalls++
        val incoming = changes.single()
        val local = incoming.row as VendorMirrorRow
        val remote = local.copy(displayName = "Remote", updatedAt = remoteVersion)
        remoteRow = remote
        return Result.success(
            MirrorPushResult(
                pushedAt = remoteVersion,
                affectedTables = emptySet(),
                rejectedChanges = listOf(
                    MirrorPushRejection(
                        change = incoming,
                        remoteRow = remote,
                        reason = MirrorPushRejectionReason.STALE_VERSION,
                    )
                ),
            )
        )
    }

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.success(MirrorPullResult(remoteVersion, emptyList()))
}

/**
 * Имитирует конкурентную правку для проверки единственной повторной отправки.
 *
 * [secondRemoteVersion] включает второй отказ, а [onFirstRejection] позволяет
 * изменить Room между первой попыткой и повторным чтением снимка.
 */
private class ConcurrentRetryGateway(
    private val firstRemoteVersion: LocalDateTime,
    private val secondRemoteVersion: LocalDateTime? = null,
    private val onFirstRejection: suspend () -> Unit = {},
) : MirrorSyncRemoteGateway {
    var snapshotCalls = 0
    var pushCalls = 0
    var remoteRow: VendorMirrorRow? = null
        private set

    override suspend fun getConfigurationStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = setOf(MirrorSyncTable.VENDOR.tableName),
        checkedAt = firstRemoteVersion,
    )

    override suspend fun getStatus() = getConfigurationStatus()

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>): Result<MirrorRemoteSnapshot> {
        snapshotCalls++
        return Result.success(
            MirrorRemoteSnapshot(
                loadedAt = firstRemoteVersion,
                rowsByTable = tables.associateWith { table ->
                    if (table == MirrorSyncTable.VENDOR) listOfNotNull(remoteRow) else emptyList()
                },
            )
        )
    }

    @Suppress("ReturnCount")
    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>): Result<MirrorPushResult> {
        pushCalls++
        val incoming = changes.single()
        val incomingRow = incoming.row as VendorMirrorRow
        if (pushCalls == 1) {
            val competing = incomingRow.copy(
                displayName = "Remote",
                updatedAt = firstRemoteVersion,
            )
            remoteRow = competing
            onFirstRejection()
            return Result.success(rejectedResult(incoming, competing))
        }

        val retryCompetingVersion = secondRemoteVersion
        if (retryCompetingVersion != null) {
            val competing = incomingRow.copy(
                displayName = "Remote after retry",
                updatedAt = retryCompetingVersion,
            )
            remoteRow = competing
            return Result.success(rejectedResult(incoming, competing))
        }

        remoteRow = incomingRow
        return Result.success(
            MirrorPushResult(
                pushedAt = incomingRow.updatedAt,
                affectedTables = setOf(MirrorSyncTable.VENDOR),
                acceptedChanges = listOf(incoming),
            )
        )
    }

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.success(MirrorPullResult(firstRemoteVersion, emptyList()))

    /** Формирует отказ push с актуальной конкурирующей строкой удалённого зеркала. */
    private fun rejectedResult(
        incoming: MirrorPushEntityChange,
        competing: VendorMirrorRow,
    ) = MirrorPushResult(
        pushedAt = competing.updatedAt,
        affectedTables = emptySet(),
        rejectedChanges = listOf(
            MirrorPushRejection(
                change = incoming,
                remoteRow = competing,
                reason = if (competing.updatedAt == incoming.row.updatedAt) {
                    MirrorPushRejectionReason.EQUAL_VERSION_CONFLICT
                } else {
                    MirrorPushRejectionReason.STALE_VERSION
                },
            )
        ),
    )
}

/**
 * Тестовый compare-and-set gateway для одной строки поставщика.
 *
 * Он принимает лишь более новую версию и умеет изменить Room после первого
 * снимка, чтобы проверить защиту локального условного применения.
 */
private class ConditionalVendorGateway(
    initialRemoteRow: VendorMirrorRow,
    private val onFirstSnapshot: suspend () -> Unit = {},
) : MirrorSyncRemoteGateway {
    var snapshotCalls = 0
    var pushCalls = 0
    var remoteRow = initialRemoteRow
        private set

    override suspend fun getConfigurationStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = setOf(MirrorSyncTable.VENDOR.tableName),
        checkedAt = remoteRow.updatedAt,
    )

    override suspend fun getStatus() = getConfigurationStatus()

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>): Result<MirrorRemoteSnapshot> {
        snapshotCalls++
        val snapshot = MirrorRemoteSnapshot(
            loadedAt = remoteRow.updatedAt,
            rowsByTable = tables.associateWith { table ->
                if (table == MirrorSyncTable.VENDOR) listOf(remoteRow) else emptyList()
            },
        )
        if (snapshotCalls == 1) onFirstSnapshot()
        return Result.success(snapshot)
    }

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>): Result<MirrorPushResult> {
        pushCalls++
        val incoming = changes.single()
        val incomingRow = incoming.row as VendorMirrorRow
        return if (incomingRow.updatedAt > remoteRow.updatedAt) {
            remoteRow = incomingRow
            Result.success(
                MirrorPushResult(
                    pushedAt = incomingRow.updatedAt,
                    affectedTables = setOf(MirrorSyncTable.VENDOR),
                    acceptedChanges = listOf(incoming),
                )
            )
        } else {
            Result.success(
                MirrorPushResult(
                    pushedAt = remoteRow.updatedAt,
                    affectedTables = emptySet(),
                    rejectedChanges = listOf(
                        MirrorPushRejection(
                            change = incoming,
                            remoteRow = remoteRow,
                            reason = MirrorPushRejectionReason.STALE_VERSION,
                        )
                    ),
                )
            )
        }
    }

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.success(MirrorPullResult(remoteRow.updatedAt, emptyList()))
}

/**
 * Имитирует конкурирующую YDB-запись при разрешении конфликта.
 *
 * Первый push всегда отклоняется и вызывает [onFirstRejection], после чего сервис
 * должен перечитать обе стороны и вернуть Doctor свежий конфликт.
 */
private class ConflictResolutionRetryGateway(
    initialRemoteRow: VendorMirrorRow,
    private val onFirstRejection: suspend (VendorMirrorRow) -> Unit,
) : MirrorSyncRemoteGateway {
    var snapshotCalls = 0
    var pushCalls = 0
    var remoteRow = initialRemoteRow
        private set

    override suspend fun getConfigurationStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = setOf(MirrorSyncTable.VENDOR.tableName),
        checkedAt = remoteRow.updatedAt,
    )

    override suspend fun getStatus() = getConfigurationStatus()

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>): Result<MirrorRemoteSnapshot> {
        snapshotCalls++
        return Result.success(
            MirrorRemoteSnapshot(
                loadedAt = remoteRow.updatedAt,
                rowsByTable = tables.associateWith { table ->
                    if (table == MirrorSyncTable.VENDOR) listOf(remoteRow) else emptyList()
                },
            )
        )
    }

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>): Result<MirrorPushResult> {
        pushCalls++
        val incoming = changes.single()
        val incomingRow = incoming.row as VendorMirrorRow
        if (pushCalls == 1) {
            val competing = incomingRow.copy(
                displayName = "Concurrent remote",
                updatedAt = defaultUpdatedAt(incomingRow.updatedAt),
            )
            remoteRow = competing
            onFirstRejection(competing)
            return Result.success(rejectedResult(incoming, competing))
        }

        remoteRow = incomingRow
        return Result.success(
            MirrorPushResult(
                pushedAt = incomingRow.updatedAt,
                affectedTables = setOf(MirrorSyncTable.VENDOR),
                acceptedChanges = listOf(incoming),
            )
        )
    }

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.success(MirrorPullResult(remoteRow.updatedAt, emptyList()))

    /** Формирует отказ push для устаревшей входящей версии. */
    private fun rejectedResult(
        incoming: MirrorPushEntityChange,
        competing: VendorMirrorRow,
    ) = MirrorPushResult(
        pushedAt = competing.updatedAt,
        affectedTables = emptySet(),
        rejectedChanges = listOf(
            MirrorPushRejection(
                change = incoming,
                remoteRow = competing,
                reason = MirrorPushRejectionReason.STALE_VERSION,
            )
        ),
    )
}

/** Преобразует локальную тестовую сущность в строку mirror без доступа к codec. */
private fun Vendor.toMirrorRowForTest() = VendorMirrorRow(
    syncId = syncId,
    displayName = displayName,
    comment = comment,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

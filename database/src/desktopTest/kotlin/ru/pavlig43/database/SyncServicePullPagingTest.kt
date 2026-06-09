package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.RemoteFileSyncState
import ru.pavlig43.database.data.sync.RemotePullChange
import ru.pavlig43.database.data.sync.RemotePullResult
import ru.pavlig43.database.data.sync.RemotePushPayload
import ru.pavlig43.database.data.sync.RemotePushResult
import ru.pavlig43.database.data.sync.RemoteSyncStatus
import ru.pavlig43.database.data.sync.BrokenRemoteSyncChange
import ru.pavlig43.database.data.sync.SyncChangeType
import ru.pavlig43.database.data.sync.SyncEntityExportRepository
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.SyncRemoteApplyRepository
import ru.pavlig43.database.data.sync.SyncRemoteGateway
import ru.pavlig43.database.data.sync.SyncRunner
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncStateEntity
import ru.pavlig43.database.data.sync.SyncStateRepository
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import ru.pavlig43.testkit.scenario

class SyncServicePullPagingTest : DesktopMainDispatcherFunSpec({

    test(
        scenario(
            given = "more remote changes than fit into one pull page",
            whenAction = "pull is executed once",
            thenResult = "the service drains every page and advances the cursor to the newest change",
        )
    ) {
        withEmptyTestDatabase { db ->
            db.syncDao.upsertSyncState(
                SyncStateEntity(
                    deviceId = "local-device",
                )
            )

            val gateway = PagingSyncRemoteGateway(totalChanges = 250)
            val service = SyncService(
                syncQueueRepository = SyncQueueRepository(db.syncDao),
                syncStateRepository = SyncStateRepository(db.syncDao),
                syncRunner = SyncRunner(db, SyncQueueRepository(db.syncDao)),
                syncEntityExportRepository = SyncEntityExportRepository(db),
                syncRemoteGateway = gateway,
                syncRemoteApplyRepository = SyncRemoteApplyRepository(db),
            )

            val result = service.pullOnce()
            val updatedState = db.syncDao.getSyncState()

            gateway.requestedCursors shouldBe listOf(null, "cursor-100", "cursor-200")
            updatedState?.lastRemoteCursor shouldBe "cursor-250"
            updatedState?.lastPullAt shouldBe result.lastPullAt
            result.status.hasRemoteChanges shouldBe false
        }
    }
})

private class PagingSyncRemoteGateway(
    totalChanges: Int,
) : SyncRemoteGateway {
    private val allChanges = (1..totalChanges).map { index ->
        RemotePullChange(
            cursor = "cursor-$index",
            sourceDeviceId = "remote-device",
            entityTable = "noop",
            entitySyncId = "entity-$index",
            changeType = SyncChangeType.UPSERT,
            changedAt = LocalDateTime(2026, 6, 9, 12, 0),
            payloadJson = null,
        )
    }

    val requestedCursors = mutableListOf<String?>()

    override suspend fun getStatus(syncState: SyncStateEntity?): RemoteSyncStatus {
        val lastCursor = syncState?.lastRemoteCursor
        val lastCursorIndex = lastCursor
            ?.substringAfter("cursor-")
            ?.toInt()
            ?: 0
        val hasRemoteChanges = allChanges.any { change ->
            change.cursor.substringAfter("cursor-").toInt() > lastCursorIndex
        }
        return RemoteSyncStatus(
            configured = true,
            hasRemoteChanges = hasRemoteChanges,
            checkedAt = LocalDateTime(2026, 6, 9, 12, 30),
            remoteCursor = lastCursor,
        )
    }

    override suspend fun loadCurrentRemoteFileStates(): Result<List<RemoteFileSyncState>> {
        return Result.success(emptyList())
    }

    override suspend fun loadBrokenRemoteChanges(): Result<List<BrokenRemoteSyncChange>> {
        return Result.success(emptyList())
    }

    override suspend fun deleteBrokenRemoteChanges(
        changes: List<BrokenRemoteSyncChange>,
    ): Result<Int> {
        return Result.success(0)
    }

    override suspend fun pushChanges(payload: RemotePushPayload): Result<RemotePushResult> {
        return Result.success(
            RemotePushResult(
                pushedAt = LocalDateTime(2026, 6, 9, 12, 0),
                remoteCursor = payload.lastRemoteCursor,
            )
        )
    }

    override suspend fun pullChanges(
        deviceId: String,
        lastRemoteCursor: String?,
        limit: Int,
    ): Result<RemotePullResult> {
        requestedCursors += lastRemoteCursor
        val startIndex = lastRemoteCursor
            ?.substringAfter("cursor-")
            ?.toInt()
            ?: 0
        val page = allChanges.drop(startIndex).take(limit)
        return Result.success(
            RemotePullResult(
                changes = page,
                pulledAt = LocalDateTime(2026, 6, 9, 12, requestedCursors.size),
                remoteCursor = page.lastOrNull()?.cursor ?: lastRemoteCursor,
            )
        )
    }
}

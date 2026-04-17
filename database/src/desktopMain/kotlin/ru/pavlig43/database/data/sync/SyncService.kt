package ru.pavlig43.database.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime

class SyncService(
    private val syncQueueRepository: SyncQueueRepository,
    private val syncStateRepository: SyncStateRepository,
    private val syncRunner: SyncRunner,
    private val syncEntityExportRepository: SyncEntityExportRepository,
    private val syncRemoteGateway: SyncRemoteGateway,
    private val syncRemoteApplyRepository: SyncRemoteApplyRepository,
) {
    private val _status = MutableStateFlow<SyncStatusSnapshot?>(null)
    val status: StateFlow<SyncStatusSnapshot?> = _status.asStateFlow()

    suspend fun getStatus(): SyncStatusSnapshot {
        val syncState = syncStateRepository.getSyncState()
        val remoteStatus = syncRemoteGateway.getStatus(syncState)

        return SyncStatusSnapshot(
            pendingChangesCount = syncQueueRepository.getChangesCount(SyncQueueStatus.PENDING),
            failedChangesCount = syncQueueRepository.getChangesCount(SyncQueueStatus.FAILED),
            hasRemoteChanges = remoteStatus.hasRemoteChanges,
            remoteSyncConfigured = remoteStatus.configured,
            lastStatusCheckAt = remoteStatus.checkedAt,
            lastSyncAt = syncState?.lastPushAt,
            lastPullAt = syncState?.lastPullAt,
            lastRemoteCursor = syncState?.lastRemoteCursor,
            payloadVersion = CURRENT_SYNC_PAYLOAD_VERSION,
            remoteError = remoteStatus.error,
        ).also { snapshot ->
            _status.value = snapshot
        }
    }

    suspend fun loadCurrentRemoteFileStates(): Result<List<RemoteFileSyncState>> {
        return syncRemoteGateway.loadCurrentRemoteFileStates()
    }

    suspend fun syncOnce(): SyncRunResult {
        val pushResult = pushOnce()
        if (pushResult.error != null) {
            return pushResult
        }

        val pullResult = pullOnce()
        if (pullResult.error != null) {
            return pullResult
        }

        return SyncRunResult(
            status = pullResult.status,
            lastSyncAt = pushResult.lastSyncAt ?: pullResult.lastSyncAt,
            lastPushAt = pushResult.lastPushAt,
            lastPullAt = pullResult.lastPullAt,
        )
    }

    suspend fun pushOnce(): SyncRunResult {
        val syncState = syncStateRepository.getSyncState()
            ?: return SyncRunResult.failure("Sync state is not initialized")

        var lastSyncAt = syncState.lastPushAt

        val batch = syncRunner.reservePendingBatch().getOrElse { throwable ->
            return SyncRunResult.failure(throwable.message ?: "Failed to reserve sync batch")
        }

        if (batch != null) {
            val pushPayload = runCatching {
                batch.toRemotePushPayload(syncState, syncEntityExportRepository)
            }.getOrElse { throwable ->
                syncRunner.markBatchFailed(batch, throwable.message)
                return SyncRunResult.failure(
                    message = throwable.message ?: "Failed to export sync batch",
                    status = getStatus(),
                )
            }

            val pushResult = syncRemoteGateway.pushChanges(pushPayload).fold(
                onSuccess = { it },
                onFailure = { throwable ->
                    syncRunner.markBatchFailed(batch, throwable.message)
                    return SyncRunResult.failure(
                        message = throwable.message ?: "Push failed",
                        status = getStatus(),
                    )
                }
            )

            syncRunner.markBatchSucceeded(batch)
            syncStateRepository.updateLastPushAt(
                pushedAt = pushResult.pushedAt,
            )
            lastSyncAt = pushResult.pushedAt
        }

        val status = getStatus()
        return SyncRunResult(
            status = status,
            lastSyncAt = lastSyncAt,
            lastPushAt = lastSyncAt,
            lastPullAt = status.lastPullAt,
        )
    }

    suspend fun pullOnce(): SyncRunResult {
        val syncState = syncStateRepository.getSyncState()
            ?: return SyncRunResult.failure("Sync state is not initialized")

        val pullResult = syncRemoteGateway.pullChanges(
            deviceId = syncState.deviceId,
            lastRemoteCursor = syncState.lastRemoteCursor,
        ).fold(
            onSuccess = { it },
            onFailure = { throwable ->
                return SyncRunResult.failure(
                    message = throwable.message ?: "Pull failed",
                    status = getStatus(),
                )
            }
        )

        runCatching {
            syncRemoteApplyRepository.applyChanges(pullResult.changes)
        }.getOrElse { throwable ->
            return SyncRunResult.failure(
                message = throwable.message ?: "Apply remote changes failed",
                status = getStatus(),
            )
        }
        syncStateRepository.updateLastPullAt(
            pulledAt = pullResult.pulledAt,
            remoteCursor = pullResult.remoteCursor,
        )

        val status = getStatus()
        return SyncRunResult(
            status = status,
            lastSyncAt = status.lastSyncAt,
            lastPushAt = status.lastSyncAt,
            lastPullAt = pullResult.pulledAt,
        )
    }
}

private suspend fun SyncPushBatch.toRemotePushPayload(
    syncState: SyncStateEntity,
    syncEntityExportRepository: SyncEntityExportRepository,
): RemotePushPayload {
    return RemotePushPayload(
        deviceId = syncState.deviceId,
        lastRemoteCursor = syncState.lastRemoteCursor,
        reservedAt = reservedAt,
        changes = buildList {
            changes.forEach { change ->
                add(syncEntityExportRepository.export(change))
            }
        }
    )
}

data class SyncStatusSnapshot(
    val pendingChangesCount: Int,
    val failedChangesCount: Int,
    val hasRemoteChanges: Boolean,
    val remoteSyncConfigured: Boolean,
    val lastStatusCheckAt: LocalDateTime,
    val lastSyncAt: LocalDateTime?,
    val lastPullAt: LocalDateTime?,
    val lastRemoteCursor: String?,
    val payloadVersion: Int,
    val remoteError: String? = null,
)

data class SyncRunResult(
    val status: SyncStatusSnapshot,
    val lastSyncAt: LocalDateTime? = null,
    val lastPushAt: LocalDateTime? = null,
    val lastPullAt: LocalDateTime? = null,
    val error: String? = null,
) {
    companion object {
        fun failure(
            message: String,
            status: SyncStatusSnapshot? = null,
        ): SyncRunResult {
            val fallbackStatus = status ?: SyncStatusSnapshot(
                pendingChangesCount = 0,
                failedChangesCount = 0,
                hasRemoteChanges = false,
                remoteSyncConfigured = false,
                lastStatusCheckAt = defaultUpdatedAt(),
                lastSyncAt = null,
                lastPullAt = null,
                lastRemoteCursor = null,
                payloadVersion = CURRENT_SYNC_PAYLOAD_VERSION,
                remoteError = null,
            )
            return SyncRunResult(
                status = fallbackStatus,
                error = message,
            )
        }
    }
}

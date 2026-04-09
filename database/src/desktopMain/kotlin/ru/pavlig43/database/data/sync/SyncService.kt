package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime

class SyncService(
    private val syncQueueRepository: SyncQueueRepository,
    private val syncStateRepository: SyncStateRepository,
    private val syncRunner: SyncRunner,
    private val syncEntityExportRepository: SyncEntityExportRepository,
    private val syncRemoteGateway: SyncRemoteGateway,
    private val syncRemoteApplyRepository: SyncRemoteApplyRepository,
) {

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
        )
    }

    suspend fun syncOnce(): SyncRunResult {
        val syncState = syncStateRepository.getSyncState()
            ?: return SyncRunResult.failure("Sync state is not initialized")

        var lastSyncAt = syncState.lastPushAt
        val pullCursorBeforePush = syncState.lastRemoteCursor

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

        val refreshedSyncState = syncStateRepository.getSyncState()
            ?: return SyncRunResult.failure("Sync state is not initialized after push")

        val pullResult = syncRemoteGateway.pullChanges(
            deviceId = refreshedSyncState.deviceId,
            lastRemoteCursor = pullCursorBeforePush,
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

        return SyncRunResult(
            status = getStatus(),
            lastSyncAt = lastSyncAt,
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
)

data class SyncRunResult(
    val status: SyncStatusSnapshot,
    val lastSyncAt: LocalDateTime? = null,
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
                remoteSyncConfigured = true,
                lastStatusCheckAt = defaultUpdatedAt(),
                lastSyncAt = null,
                lastPullAt = null,
                lastRemoteCursor = null,
                payloadVersion = CURRENT_SYNC_PAYLOAD_VERSION,
            )
            return SyncRunResult(
                status = fallbackStatus,
                error = message,
            )
        }
    }
}

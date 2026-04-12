package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime

interface SyncRemoteGateway {
    suspend fun getStatus(syncState: SyncStateEntity?): RemoteSyncStatus

    suspend fun loadCurrentRemoteFileStates(): Result<List<RemoteFileSyncState>>

    suspend fun pushChanges(
        payload: RemotePushPayload,
    ): Result<RemotePushResult>

    suspend fun pullChanges(
        deviceId: String,
        lastRemoteCursor: String?,
        limit: Int = 100,
    ): Result<RemotePullResult>
}

data class RemotePushPayload(
    val deviceId: String,
    val lastRemoteCursor: String?,
    val reservedAt: LocalDateTime,
    val changes: List<RemotePushChange>,
)

data class RemotePushChange(
    val entityTable: String,
    val entityLocalId: String,
    val changeType: SyncChangeType,
    val sourceQueueIds: List<Long>,
    val lastQueuedAt: LocalDateTime,
    val payloadJson: String?,
    val reminderEmailSource: ReminderEmailSourceChange? = null,
)

data class ReminderEmailSourceChange(
    val reminderSyncId: String,
    val transactionSyncId: String,
    val transactionType: String,
    val transactionCreatedAt: LocalDateTime,
    val reminderText: String?,
    val reminderAt: LocalDateTime?,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

data class RemoteSyncStatus(
    val configured: Boolean,
    val hasRemoteChanges: Boolean,
    val checkedAt: LocalDateTime = defaultUpdatedAt(),
    val remoteCursor: String? = null,
    val error: String? = null,
)

data class RemotePushResult(
    val pushedAt: LocalDateTime,
    val remoteCursor: String? = null,
)

data class RemotePullResult(
    val changes: List<RemotePullChange>,
    val pulledAt: LocalDateTime,
    val remoteCursor: String? = null,
)

data class RemotePullChange(
    val cursor: String,
    val sourceDeviceId: String,
    val entityTable: String,
    val entitySyncId: String,
    val changeType: SyncChangeType,
    val changedAt: LocalDateTime,
    val payloadJson: String?,
)

data class RemoteFileSyncState(
    val syncId: String,
    val remoteObjectKey: String?,
    val changeType: SyncChangeType,
    val deletedAt: LocalDateTime? = null,
)

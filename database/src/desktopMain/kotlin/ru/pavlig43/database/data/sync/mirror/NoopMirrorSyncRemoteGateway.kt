package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.datetime.getCurrentLocalDateTime

/**
 * Явная реализация gateway для установки без YDB-конфигурации.
 *
 * Status остается доступным для UI, а операции данных завершаются failure вместо
 * молчаливого no-op, чтобы вызывающий код не считал синхронизацию успешной.
 */
class NoopMirrorSyncRemoteGateway : MirrorSyncRemoteGateway {
    override suspend fun getConfigurationStatus() = getStatus()

    override suspend fun getStatus() = MirrorRemoteStatus(
        configured = false,
        availableTables = emptySet(),
        checkedAt = getCurrentLocalDateTime(),
        error = "Mirror sync is not configured",
    )

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>) =
        Result.failure<MirrorRemoteSnapshot>(IllegalStateException("Mirror sync is not configured"))

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>) =
        Result.failure<MirrorPushResult>(IllegalStateException("Mirror sync is not configured"))

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.failure<MirrorPullResult>(IllegalStateException("Mirror sync is not configured"))
}

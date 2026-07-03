package ru.pavlig43.nocombro.mobile.sync

import java.io.File
import kotlinx.datetime.LocalDateTime

/**
 * Оркестрирует Android sync: YDB snapshot, local apply, S3 upload/download.
 */
class MobileSyncRepository(
    private val configRepository: MobileRemoteConfigRepository,
    private val localRepository: MobileLocalMirrorRepository,
    private val planner: MobileReconciliationPlanner = MobileReconciliationPlanner(),
) {
    private var lastPushAt: LocalDateTime? = null
    private var lastPullAt: LocalDateTime? = null

    /**
     * Строит preview расхождений без записи в YDB, Room или S3.
     */
    suspend fun preview(): MobileSyncPreview {
        val context = loadContext().getOrElse { throwable ->
            return MobileSyncPreview(
                localChanges = emptyList(),
                remoteChanges = emptyList(),
                error = throwable.message ?: "Sync config не найден",
            )
        }
        val local = localRepository.loadSnapshot(context.config.s3)
        val remote = context.remote.loadSnapshot().getOrElse { throwable ->
            return MobileSyncPreview(
                localChanges = emptyList(),
                remoteChanges = emptyList(),
                error = throwable.message ?: "YDB snapshot не загружен",
            )
        }.mobileOnly()
        val plan = planner.plan(local, remote)
        return MobileSyncPreview(
            localChanges = buildExperimentChangeGroups(
                changes = plan.pushChanges,
                before = remote,
                after = local,
            ),
            remoteChanges = buildExperimentChangeGroups(
                changes = plan.pullChanges,
                before = local,
                after = remote,
            ),
        )
    }

    /**
     * Проверяет YDB/S3 config и считает pending local/remote changes.
     */
    suspend fun check(): MobileSyncRunResult {
        val context = loadContext().getOrElse { throwable ->
            return failure(throwable.message ?: "Sync config не найден")
        }
        val status = context.remote.status()
        if (status.error != null) return MobileSyncRunResult(status = status, error = status.error)
        val local = localRepository.loadSnapshot(context.config.s3)
        val remote = context.remote.loadSnapshot().getOrElse { throwable ->
            return failure(throwable.message ?: "YDB snapshot не загружен")
        }.mobileOnly()
        val plan = planner.plan(local, remote)
        return MobileSyncRunResult(
            status = status.copy(
                localChanges = plan.pushChanges.size,
                remoteChanges = plan.pullChanges.size,
            ),
        )
    }

    /**
     * Отправляет local winners и связанные файлы в remote.
     */
    suspend fun push(): MobileSyncRunResult {
        val context = loadContext().getOrElse { throwable ->
            return failure(throwable.message ?: "Sync config не найден")
        }
        val local = localRepository.loadSnapshot(context.config.s3)
        val remote = context.remote.loadSnapshot().getOrElse { throwable ->
            return failure(throwable.message ?: "YDB snapshot не загружен")
        }.mobileOnly()
        val plan = planner.plan(local, remote)
        uploadPushFiles(plan.pushChanges, context.storage).getOrElse { throwable ->
            return failure(throwable.message ?: "S3 upload failed")
        }
        context.remote.push(plan.pushChanges).getOrElse { throwable ->
            return failure(throwable.message ?: "YDB push failed")
        }
        lastPushAt = local.loadedAt
        return check().copy(pushed = plan.pushChanges.size, lastPushAt = lastPushAt)
    }

    /**
     * Применяет remote winners локально и скачивает недостающие файлы.
     */
    suspend fun pull(): MobileSyncRunResult {
        val context = loadContext().getOrElse { throwable ->
            return failure(throwable.message ?: "Sync config не найден")
        }
        val local = localRepository.loadSnapshot(context.config.s3)
        val remote = context.remote.loadSnapshot().getOrElse { throwable ->
            return failure(throwable.message ?: "YDB snapshot не загружен")
        }.mobileOnly()
        val plan = planner.plan(local, remote)
        localRepository.applyRemoteChanges(plan.pullChanges)
        lastPullAt = remote.loadedAt
        downloadMissingFiles(context.storage).getOrElse { throwable ->
            return failure(throwable.message ?: "S3 download failed")
        }
        return check().copy(
            pulled = plan.pullChanges.size,
            lastPullAt = lastPullAt,
        )
    }

    /**
     * Выполняет push, затем pull; при ошибке push останавливает сценарий.
     */
    suspend fun sync(): MobileSyncRunResult {
        val pushed = push()
        if (pushed.error != null) return pushed
        val pulled = pull()
        return pulled.copy(
            pushed = pushed.pushed,
            lastPushAt = pushed.lastPushAt,
        )
    }

    private fun loadContext(): Result<MobileSyncContext> = runCatching {
        val config = configRepository.load().getOrThrow()
        val serviceAccountJson = configRepository.decodeServiceAccountJson(config.ydb)
        MobileSyncContext(
            config = config,
            remote = MobileYdbMirrorGateway(config.ydb, serviceAccountJson),
            storage = AwsKotlinMobileS3Gateway(config.s3),
        )
    }

    private suspend fun uploadPushFiles(
        changes: List<MobileMirrorChange>,
        storage: MobileObjectStorageGateway,
    ): Result<Unit> = runCatching {
        changes.asSequence()
            .map(MobileMirrorChange::row)
            .filterIsInstance<MobileFileMirrorRow>()
            .filter { it.deletedAt == null }
            .forEach { file ->
                val remoteKey = file.remoteObjectKey ?: return@forEach
                storage.uploadFile(file.path, remoteKey).getOrThrow()
            }
    }

    private suspend fun downloadMissingFiles(
        storage: MobileObjectStorageGateway,
    ): Result<Unit> = runCatching {
        val snapshot = localRepository.loadSnapshot(
            configRepository.load().getOrThrow().s3,
        )
        val files = snapshot.rowsByTable[MobileMirrorTable.FILE].orEmpty()
            .filterIsInstance<MobileFileMirrorRow>()
            .filter { (it.deletedAt == null) && (it.remoteObjectKey != null) }
            .filterNot { File(it.path).isFile }
        files.forEach { file ->
            storage.downloadFile(file.remoteObjectKey.orEmpty(), file.path).getOrThrow()
        }
    }

    private fun failure(message: String): MobileSyncRunResult {
        return MobileSyncRunResult(
            status = MobileSyncStatus(
                configured = false,
                checkedAt = ru.pavlig43.datetime.getCurrentLocalDateTime(),
                error = message,
            ),
            error = message,
        )
    }
}

/**
 * Runtime-контекст одной sync-операции.
 */
private data class MobileSyncContext(
    val config: MobileRemoteConfig,
    val remote: MobileYdbMirrorGateway,
    val storage: MobileObjectStorageGateway,
)

/**
 * Оставляет только rows, которые Android умеет показать и применить.
 */
private fun MobileMirrorSnapshot.mobileOnly(): MobileMirrorSnapshot {
    val experimentIds = rowsByTable[MobileMirrorTable.EXPERIMENT].orEmpty()
        .mapTo(mutableSetOf(), MobileMirrorRow::syncId)
    val entryRows = rowsByTable[MobileMirrorTable.EXPERIMENT_ENTRY].orEmpty()
        .filterIsInstance<MobileExperimentEntryMirrorRow>()
        .filter { it.experimentSyncId in experimentIds }
    val entryIds = entryRows.mapTo(mutableSetOf(), MobileMirrorRow::syncId)
    val reminderRows = rowsByTable[MobileMirrorTable.EXPERIMENT_REMINDER].orEmpty()
        .filterIsInstance<MobileExperimentReminderMirrorRow>()
        .filter { it.experimentSyncId in experimentIds }
    val fileRows = rowsByTable[MobileMirrorTable.FILE].orEmpty()
        .filterIsInstance<MobileFileMirrorRow>()
        .filter {
            (it.ownerType == MobileFileOwnerType.EXPERIMENT_ENTRY) &&
                (it.ownerSyncId in entryIds)
        }
    return copy(
        rowsByTable = rowsByTable + mapOf(
            MobileMirrorTable.EXPERIMENT_ENTRY to entryRows,
            MobileMirrorTable.EXPERIMENT_REMINDER to reminderRows,
            MobileMirrorTable.FILE to fileRows,
        ),
    )
}

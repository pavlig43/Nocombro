package ru.pavlig43.nocombro.mobile.sync

import java.io.File
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime

/**
 * Оркестрирует Android-синхронизацию: YDB, локальную Room-БД и S3.
 *
 * Алгоритм всегда строится вокруг двух снимков: локального и удалённого.
 * `MobileReconciliationPlanner` выбирает более новую строку по
 * `updatedAt/deletedAt`, после чего репозиторий
 * либо отправляет локальные строки в YDB, либо применяет удалённые строки в Room.
 * Бинарные файлы идут отдельно: сначала метаданные в YDB, затем загрузка или
 * скачивание в S3 по ключам из строк `file`.
 */
class MobileSyncRepository(
    private val configRepository: MobileRemoteConfigSource,
    private val localRepository: MobileLocalMirrorDataSource,
    private val planner: MobileReconciliationPlanner = MobileReconciliationPlanner(),
    private val remoteGatewayFactory: (MobileYdbConfig, String?) -> MobileRemoteMirrorGateway =
        ::MobileYdbMirrorGateway,
    private val storageGatewayFactory: (MobileS3Config) -> MobileObjectStorageGateway =
        ::AwsKotlinMobileS3Gateway,
) {
    private var lastPushAt: LocalDateTime? = null
    private var lastPullAt: LocalDateTime? = null

    /**
     * Проверяет настройки YDB/S3 и считает, сколько строк ждёт отправки и получения.
     */
    suspend fun check(): MobileSyncRunResult {
        val context = loadContext().getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Настройки синхронизации не найдены"))
        }
        val local = localRepository.loadSnapshot(context.config.s3)
        val remote = context.remote.loadSnapshot().mapCatching { snapshot ->
            snapshot.mobileOnly().normalizeFileKeys(context.config.s3)
        }.getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Не удалось получить снимок YDB"))
        }
        val plan = planner.plan(local, remote)
        val preview = MobileSyncPreview(
            snapshotAt = remote.loadedAt,
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
            error = MOBILE_SYNC_CONFLICT_HINT.takeIf { plan.conflicts.isNotEmpty() },
        )
        return MobileSyncRunResult(
            status = MobileSyncStatus(
                configured = true,
                checkedAt = remote.loadedAt,
                localChanges = plan.pushChanges.size,
                remoteChanges = plan.pullChanges.size,
                conflicts = plan.conflicts,
            ),
            preview = preview,
        )
    }

    /**
     * Отправляет локальные версии в YDB и связанные файлы в S3.
     *
     * Сначала грузит бинарные файлы, затем условно пишет метаданные в YDB. Если
     * удалённая версия успела измениться после снимка, репозиторий один раз заново
     * читает оба снимка и строит новый план. Вторая отклонённая запись завершает
     * операцию ошибкой: бесконечные повторы могли бы скрыть постоянную конкуренцию.
     *
     * @return число принятых YDB строк и план, который остался после записи.
     */
    suspend fun push(): MobileSyncRunResult = pushInternal(pullAfterPush = false)

    private suspend fun pushInternal(
        pullAfterPush: Boolean,
    ): MobileSyncRunResult {
        val context = loadContext().getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Настройки синхронизации не найдены"))
        }
        val local = localRepository.loadSnapshot(context.config.s3)
        val remote = context.remote.loadSnapshot().mapCatching { snapshot ->
            snapshot.mobileOnly().normalizeFileKeys(context.config.s3)
        }.getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Не удалось получить снимок YDB"))
        }
        val plan = planner.plan(local, remote)
        uploadPushFiles(plan.pushChanges, context.storage).getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Не удалось загрузить файлы в S3"))
        }
        val pushResult = context.remote.push(plan.pushChanges).getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Не удалось отправить данные в YDB"))
        }
        val acceptedChanges = pushResult.acceptedChanges.toMutableList()
        val postPushPlan = if (pushResult.rejectedChanges.isNotEmpty()) {
            val refreshedRemote = context.remote.loadSnapshot().mapCatching { snapshot ->
                snapshot.mobileOnly().normalizeFileKeys(context.config.s3)
            }.getOrElse { throwable ->
                return failure(throwable.mobileSyncErrorMessage("Не удалось повторно получить снимок YDB"))
            }
            val refreshedLocal = localRepository.loadSnapshot(context.config.s3)
            val refreshedPlan = planner.plan(refreshedLocal, refreshedRemote)
            if (refreshedPlan.pushChanges.isEmpty()) {
                refreshedPlan
            } else {
                uploadPushFiles(refreshedPlan.pushChanges, context.storage).getOrElse { throwable ->
                    return failure(throwable.mobileSyncErrorMessage("Не удалось повторно загрузить файлы в S3"))
                }
                val retryResult = context.remote.push(refreshedPlan.pushChanges).getOrElse { throwable ->
                    return failure(throwable.mobileSyncErrorMessage("Не удалось повторно отправить данные в YDB"))
                }
                if (retryResult.rejectedChanges.isNotEmpty()) {
                    return failure(retryResult.rejectedChanges.secondRejectionMessage())
                }
                acceptedChanges += retryResult.acceptedChanges
                planner.plan(
                    refreshedLocal,
                    refreshedRemote.withAppliedChanges(retryResult.acceptedChanges),
                )
            }
        } else {
            planner.plan(local, remote.withAppliedChanges(pushResult.acceptedChanges))
        }
        lastPushAt = getCurrentLocalDateTime()
        val pulledChanges = if (pullAfterPush) {
            localRepository.applyRemoteChanges(postPushPlan.pullChanges, context.config.s3)
            lastPullAt = getCurrentLocalDateTime()
            downloadMissingFiles(context.storage).getOrElse { throwable ->
                return failure(throwable.mobileSyncErrorMessage("Не удалось скачать файлы из S3")).copy(
                    pushed = acceptedChanges.size,
                    pulled = postPushPlan.pullChanges.size,
                    lastPushAt = lastPushAt,
                    lastPullAt = lastPullAt,
                )
            }
            postPushPlan.pullChanges.size
        } else {
            0
        }
        return MobileSyncRunResult(
            status = MobileSyncStatus(
                configured = true,
                checkedAt = getCurrentLocalDateTime(),
                localChanges = postPushPlan.pushChanges.size,
                remoteChanges = if (pullAfterPush) {
                    0
                } else {
                    postPushPlan.pullChanges.size
                },
                conflicts = postPushPlan.conflicts,
            ),
            pushed = acceptedChanges.size,
            pulled = pulledChanges,
            lastPushAt = lastPushAt,
            lastPullAt = lastPullAt.takeIf { pullAfterPush },
        )
    }

    /**
     * Применяет удалённые версии локально и скачивает недостающие файлы.
     *
     * Получение сначала пишет метаданные в Room. После этого можно понять, каких файлов
     * нет на телефоне, и скачать их из S3 по логическим ключам.
     */
    suspend fun pull(): MobileSyncRunResult {
        val context = loadContext().getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Настройки синхронизации не найдены"))
        }
        val local = localRepository.loadSnapshot(context.config.s3)
        val remote = context.remote.loadSnapshot().mapCatching { snapshot ->
            snapshot.mobileOnly().normalizeFileKeys(context.config.s3)
        }.getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Не удалось получить снимок YDB"))
        }
        val plan = planner.plan(local, remote)
        localRepository.applyRemoteChanges(plan.pullChanges, context.config.s3)
        lastPullAt = remote.loadedAt
        downloadMissingFiles(context.storage).getOrElse { throwable ->
            return failure(throwable.mobileSyncErrorMessage("Не удалось скачать файлы из S3"))
        }
        return MobileSyncRunResult(
            status = MobileSyncStatus(
                configured = true,
                checkedAt = remote.loadedAt,
                localChanges = plan.pushChanges.size,
                remoteChanges = 0,
                conflicts = plan.conflicts,
            ),
            pulled = plan.pullChanges.size,
            lastPullAt = lastPullAt,
        )
    }

    /**
     * Выполняет отправку и получение из одной пары снимков Room и YDB.
     *
     * Если отправка завершилась ошибкой, получение не начинается: иначе пользователь увидит смешанный
     * результат, где часть локальных файлов не ушла в S3, но новые удалённые
     * строки уже применились.
     */
    suspend fun sync(): MobileSyncRunResult = pushInternal(pullAfterPush = true)

    private fun loadContext(): Result<MobileSyncContext> = runCatching {
        val config = configRepository.load().getOrThrow()
        val serviceAccountJson = configRepository.decodeServiceAccountJson(config.ydb)
        MobileSyncContext(
            config = config,
            remote = remoteGatewayFactory(config.ydb, serviceAccountJson),
            storage = storageGatewayFactory(config.s3),
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
                storage.uploadFile(file.path, remoteKey).getOrElse { throwable ->
                    throw MobileSyncOperationException(
                        message = "table=${MobileMirrorTable.FILE.tableName}, sync_id=${file.syncId}",
                        cause = throwable,
                    )
                }
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
            storage.downloadFile(file.remoteObjectKey.orEmpty(), file.path).getOrElse { throwable ->
                throw MobileSyncOperationException(
                    message = "table=${MobileMirrorTable.FILE.tableName}, sync_id=${file.syncId}",
                    cause = throwable,
                )
            }
        }
    }

    private fun failure(message: String): MobileSyncRunResult {
        return MobileSyncRunResult(
            status = MobileSyncStatus(
                configured = false,
                checkedAt = getCurrentLocalDateTime(),
                error = message,
            ),
            error = message,
        )
    }
}

/**
 * Контекст одной операции синхронизации.
 */
private data class MobileSyncContext(
    val config: MobileRemoteConfig,
    val remote: MobileRemoteMirrorGateway,
    val storage: MobileObjectStorageGateway,
)

/**
 * Оставляет только строки, которые Android умеет показать и применить.
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

/**
 * Нормализует ключи файлов из YDB.
 *
 * Старые строки могли хранить ключ уже с S3-префиксом. Android хранит и сравнивает
 * только логический ключ, чтобы следующая отправка не записала `prefix/prefix/...`.
 */
internal fun MobileMirrorSnapshot.normalizeFileKeys(config: MobileS3Config): MobileMirrorSnapshot {
    val normalizedFileRows = rowsByTable[MobileMirrorTable.FILE].orEmpty()
        .map { row ->
            if (row is MobileFileMirrorRow) {
                row.copy(
                    remoteObjectKey = row.remoteObjectKey?.let { objectKey ->
                        runCatching { config.normalizeObjectKey(objectKey) }
                            .getOrElse { throwable ->
                                throw MobileSyncOperationException(
                                    message = "table=${MobileMirrorTable.FILE.tableName}, " +
                                        "sync_id=${row.syncId}",
                                    cause = throwable,
                                )
                            }
                    },
                )
            } else {
                row
            }
        }
    return copy(rowsByTable = rowsByTable + (MobileMirrorTable.FILE to normalizedFileRows))
}

/** Текст для конфликтов, которые мобильный клиент не умеет разрешать. */
internal const val MOBILE_SYNC_CONFLICT_HINT =
    "Найдены конфликты данных. Откройте Doctor в настольном приложении."

/**
 * Возвращает копию удалённого снимка с уже принятыми строками.
 *
 * Функция нужна для расчёта статуса сразу после отправки без ещё одного сетевого
 * чтения. Строки заменяются по паре «таблица, `syncId`».
 */
private fun MobileMirrorSnapshot.withAppliedChanges(
    changes: List<MobileMirrorChange>,
): MobileMirrorSnapshot {
    if (changes.isEmpty()) return this
    val changesByTable = changes.groupBy(MobileMirrorChange::table)
    return copy(
        rowsByTable = rowsByTable + changesByTable.mapValues { (table, tableChanges) ->
            val rowsBySyncId = rowsByTable[table].orEmpty()
                .associateByTo(linkedMapOf(), MobileMirrorRow::syncId)
            tableChanges.forEach { change -> rowsBySyncId[change.row.syncId] = change.row }
            rowsBySyncId.values.toList()
        },
    )
}

/**
 * Строит безопасную ошибку повторного отказа без содержимого пользовательских полей.
 */
private fun List<MobilePushRejection>.secondRejectionMessage(): String {
    val rows = joinToString { rejection ->
        "table=${rejection.change.table.tableName}, sync_id=${rejection.change.row.syncId}"
    }
    return "YDB push отклонён после одной повторной проверки: $rows"
}

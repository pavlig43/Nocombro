package ru.pavlig43.doctor.internal.component

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRow
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.files.api.LocalFilesStorageOverview
import ru.pavlig43.files.api.model.LocalOrphanFile
import ru.pavlig43.files.api.model.RemoteOrphanFile

enum class DoctorTool(
    val title: String,
    val subtitle: String,
) {
    StorageOverview(
        title = "Обзор хранилища",
        subtitle = "Показать размер локального каталога, число файлов и orphan-файлы.",
    ),
    FileCleanup(
        title = "Чистка файлов",
        subtitle = "Найти и удалить orphan-файлы из локального каталога приложения.",
    ),
    RemoteFileCleanup(
        title = "Чистка S3",
        subtitle = "Найти и удалить orphan-объекты из удалённого bucket.",
    ),
    /** Ручной выбор содержимого для строк с равной логической версией. */
    SyncConflicts(
        title = "Конфликты sync",
        subtitle = "Выбрать local или remote для строк с равной версией.",
    ),
}

sealed interface DoctorOrphanFilesLoadState {
    data object Loading : DoctorOrphanFilesLoadState
    data class Error(val message: String) : DoctorOrphanFilesLoadState
    data class Success(val files: List<LocalOrphanFile>) : DoctorOrphanFilesLoadState
}

sealed interface DoctorStorageOverviewLoadState {
    data object Loading : DoctorStorageOverviewLoadState
    data class Error(val message: String) : DoctorStorageOverviewLoadState
    data class Success(val overview: LocalFilesStorageOverview) : DoctorStorageOverviewLoadState
}

sealed interface DoctorRemoteOrphanFilesLoadState {
    data object Idle : DoctorRemoteOrphanFilesLoadState
    data object Loading : DoctorRemoteOrphanFilesLoadState
    data class Error(val message: String) : DoctorRemoteOrphanFilesLoadState
    data class Success(val files: List<RemoteOrphanFile>) : DoctorRemoteOrphanFilesLoadState
}

/**
 * Готовое для UI представление одного mirror-конфликта.
 *
 * [source] хранит исходные typed строки для действия, а остальные поля отделяют
 * сериализацию и расчёт различий от Compose-кода.
 */
internal data class DoctorSyncConflictView(
    val source: MirrorVersionConflict,
    val table: String,
    val syncId: String,
    val localVersion: LocalDateTime,
    val remoteVersion: LocalDateTime,
    val localStatus: DoctorMirrorRowStatus,
    val remoteStatus: DoctorMirrorRowStatus,
    val differences: List<DoctorSyncFieldDifference>,
)

/** Различие одного сериализованного поля локальной и удалённой строк. */
internal data class DoctorSyncFieldDifference(
    val field: String,
    val localValue: String,
    val remoteValue: String,
)

/** Пользовательский статус активной строки или tombstone. */
internal enum class DoctorMirrorRowStatus(val title: String) {
    ACTIVE("активна"),
    DELETED("удалена"),
}

/**
 * Строит стабильное UI-представление через kotlinx.serialization без reflection.
 *
 * Дискриминатор sealed-типа скрывается, поля сортируются по имени, а `null`, строки
 * и прочие JSON-значения получают однозначный текст. Версия каждой стороны равна
 * более позднему из `updatedAt` и `deletedAt`.
 *
 * @param json сериализатор с тем же class discriminator, что и mirror-журнал.
 */
internal fun MirrorVersionConflict.toDoctorView(
    json: Json = DOCTOR_MIRROR_JSON,
): DoctorSyncConflictView {
    val localJson = json.encodeToJsonElement<MirrorSyncRow>(localRow).jsonObject
    val remoteJson = json.encodeToJsonElement<MirrorSyncRow>(remoteRow).jsonObject
    val differences = (localJson.keys + remoteJson.keys)
        .asSequence()
        .filterNot { it == MIRROR_CLASS_DISCRIMINATOR }
        .filter { field -> localJson[field] != remoteJson[field] }
        .sorted()
        .map { field ->
            DoctorSyncFieldDifference(
                field = field,
                localValue = localJson[field].toDisplayValue(),
                remoteValue = remoteJson[field].toDisplayValue(),
            )
        }
        .toList()

    return DoctorSyncConflictView(
        source = this,
        table = table.tableName,
        syncId = localRow.syncId,
        localVersion = localRow.versionAtForDoctor(),
        remoteVersion = remoteRow.versionAtForDoctor(),
        localStatus = localRow.doctorStatus(),
        remoteStatus = remoteRow.doctorStatus(),
        differences = differences,
    )
}

/** Возвращает фактическую логическую версию строки для карточки Doctor. */
private fun MirrorSyncRow.versionAtForDoctor(): LocalDateTime =
    deletedAt?.takeIf { it > updatedAt } ?: updatedAt

/** Преобразует наличие tombstone в короткий статус для UI. */
private fun MirrorSyncRow.doctorStatus(): DoctorMirrorRowStatus =
    if (deletedAt == null) DoctorMirrorRowStatus.ACTIVE else DoctorMirrorRowStatus.DELETED

/** Преобразует JSON-значение поля в устойчивый текст без лишних кавычек у строк. */
private fun JsonElement?.toDisplayValue(): String = when (this) {
    null, JsonNull -> "null"
    is JsonPrimitive -> if (isString) content else toString()
    else -> toString()
}

private const val MIRROR_CLASS_DISCRIMINATOR = "_mirrorType"
private val DOCTOR_MIRROR_JSON = Json {
    classDiscriminator = MIRROR_CLASS_DISCRIMINATOR
}

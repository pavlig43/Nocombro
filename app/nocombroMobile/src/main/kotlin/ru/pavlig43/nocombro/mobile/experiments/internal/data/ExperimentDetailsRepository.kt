package ru.pavlig43.nocombro.mobile.experiments.internal.data

import androidx.room.withTransaction
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitOpenFileSettings
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.write
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.sync.mobileUpdatedAt
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperiment
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentEntry
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentEntryFile
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentReminder
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryFileEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentReminderEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.toModel

/**
 * Управляет содержимым одного эксперимента в мобильной Room-БД.
 *
 * Репозиторий хранит эксперимент, записи журнала, напоминания и метаданные файлов.
 * Каждая правка получает монотонно растущую UTC-версию через [mobileUpdatedAt],
 * чтобы сверка с YDB не зависела от перевода часов на устройстве. Изменение дочерней
 * строки не меняет версию эксперимента: порядок списка считается отдельным SQL-запросом.
 *
 * @param db база мобильного приложения.
 * @param filesDirPath корень приватного каталога приложения для копий вложений.
 * @param fileProviderAuthority authority Android FileProvider для открытия вложений.
 */
@Suppress("TooManyFunctions")
class ExperimentDetailsRepository(
    private val db: NocombroMobileDatabase,
    private val filesDirPath: String,
    private val fileProviderAuthority: String,
) {
    private val experimentDao = db.experimentDao
    private val entryDao = db.experimentEntryDao
    private val entryFileDao = db.experimentEntryFileDao
    private val reminderDao = db.experimentReminderDao

    fun observeExperiment(id: Int): Flow<MobileExperiment?> {
        return experimentDao.observeExperiment(id)
            .map { experiment -> experiment?.toModel() }
    }

    fun observeEntries(experimentId: Int): Flow<List<MobileExperimentEntry>> {
        return entryDao.observeEntries(experimentId)
            .map { entries -> entries.map(MobileExperimentEntryEntity::toModel) }
    }

    fun observeReminders(experimentId: Int): Flow<List<MobileExperimentReminder>> {
        return reminderDao.observeReminders(experimentId)
            .map { reminders -> reminders.map(MobileExperimentReminderEntity::toModel) }
    }

    fun observeEntry(entryId: Int): Flow<MobileExperimentEntry> {
        return entryDao.observeEntry(entryId)
            .filterNotNull()
            .map { entry -> entry.toModel() }
    }

    fun observeEntryFiles(entryId: Int): Flow<List<MobileExperimentEntryFile>> {
        return entryFileDao.observeFiles(entryId)
            .map { files -> files.map(MobileExperimentEntryFileEntity::toModel) }
    }

    /**
     * Сохраняет заголовок и описание идеи, повышая версию эксперимента.
     *
     * @param experimentId локальный идентификатор эксперимента.
     * @param title новый заголовок.
     * @param ideaDescription новое описание идеи.
     * @return успех или ошибка поиска и записи строки.
     */
    suspend fun updateExperimentDraft(
        experimentId: Int,
        title: String,
        ideaDescription: String,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val experiment = requireExperiment(experimentId)
            experimentDao.upsert(
                experiment.copy(
                    title = title,
                    ideaDescription = ideaDescription,
                    updatedAt = mobileUpdatedAt(experiment.updatedAt),
                )
            )
        }
    }

    /**
     * Меняет признак архива и создаёт новую версию строки для синхронизации.
     *
     * @param experimentId локальный идентификатор эксперимента.
     * @param isArchived новое состояние архива.
     * @return успех или ошибка поиска и записи строки.
     */
    suspend fun setExperimentArchived(
        experimentId: Int,
        isArchived: Boolean,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val experiment = requireExperiment(experimentId)
            experimentDao.upsert(
                experiment.copy(
                    isArchived = isArchived,
                    updatedAt = mobileUpdatedAt(experiment.updatedAt),
                )
            )
        }
    }

    /**
     * Создаёт запись журнала на указанную дату.
     *
     * Пользовательская дата и [MobileExperimentEntryEntity.createdAt] сохраняют
     * локальную семантику, а `updatedAt` записывается как UTC-версия синхронизации.
     *
     * @param experimentId локальный идентификатор родительского эксперимента.
     * @param entryDate дата записи журнала.
     * @return созданная модель с выданным Room идентификатором либо ошибка записи.
     */
    suspend fun createEntryForDate(
        experimentId: Int,
        entryDate: LocalDate,
    ): Result<MobileExperimentEntry> = runCatching {
        db.withTransaction {
            val updatedAt = mobileUpdatedAt()
            val entry = MobileExperimentEntryEntity(
                experimentId = experimentId,
                entryDate = entryDate,
                createdAt = getCurrentLocalDateTime(),
                syncId = UUID.randomUUID().toString(),
                updatedAt = updatedAt,
            )
            val id = entryDao.create(entry).toInt()
            entry.copy(id = id).toModel()
        }
    }

    /**
     * Обновляет текст записи и повышает только версию этой записи.
     *
     * @param entryId локальный идентификатор записи.
     * @param content новый текст записи.
     * @return успех или ошибка поиска и записи строки.
     */
    suspend fun updateEntryContent(
        entryId: Int,
        content: String,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val entry = requireNotNull(entryDao.getEntry(entryId)) {
                "Entry $entryId not found"
            }
            val now = mobileUpdatedAt(entry.updatedAt)
            entryDao.upsert(
                entry.copy(
                    content = content,
                    updatedAt = now,
                )
            )
        }
    }

    /**
     * Копирует вложение в каталог приложения и создаёт его sync-метаданные.
     *
     * Логический ключ строится из нового `syncId` и безопасного имени. Строка файла
     * создаётся лишь после успешного копирования, а версия родительской записи не
     * меняется.
     *
     * @param entryId локальный идентификатор записи-владельца.
     * @param sourceFile выбранный пользователем исходный файл.
     * @return метаданные созданного вложения либо ошибка копирования или записи.
     */
    suspend fun addEntryFile(
        entryId: Int,
        sourceFile: PlatformFile,
    ): Result<MobileExperimentEntryFile> = runCatching {
        val now = mobileUpdatedAt()
        val fileSyncId = UUID.randomUUID().toString()
        val safeName = sourceFile.name.toSafeFileName()
        val objectKey = "files/experiment_entry/$fileSyncId/$safeName"
        val localFile = File(filesDirPath, "nocombro/$objectKey")
        localFile.parentFile?.mkdirs()

        PlatformFile(localFile.absolutePath).write(sourceFile)

        db.withTransaction {
            requireEntry(entryId)
            val file = MobileExperimentEntryFileEntity(
                entryId = entryId,
                displayName = sourceFile.name,
                localPath = localFile.absolutePath,
                objectKey = objectKey,
                mimeType = sourceFile.mimeType()?.toString(),
                sizeBytes = sourceFile.size().takeIf { size -> size >= 0L },
                syncId = fileSyncId,
                updatedAt = now,
            )
            val id = entryFileDao.create(file).toInt()
            file.copy(id = id).toModel()
        }
    }

    suspend fun openEntryFile(fileId: Int): Result<Unit> = runCatching {
        val file = requireNotNull(entryFileDao.getFile(fileId)) {
            "File $fileId not found"
        }
        require(file.deletedAt == null) {
            "Файл удалён"
        }
        require(File(file.localPath).exists()) {
            "Локальный файл не найден"
        }
        FileKit.openFileWithDefaultApplication(
            file = PlatformFile(file.localPath),
            openFileSettings = FileKitOpenFileSettings(
                authority = fileProviderAuthority,
            ),
        )
    }

    /**
     * Помечает вложение tombstone и удаляет его локальную копию.
     *
     * Метаданные остаются в Room до отправки удаления в YDB. Отметка удаления
     * всегда новее прежней версии строки.
     *
     * @param fileId локальный идентификатор вложения.
     * @return успех либо ошибка поиска и обновления метаданных.
     */
    suspend fun deleteEntryFile(fileId: Int): Result<Unit> = runCatching {
        val localPath = db.withTransaction {
            val file = requireNotNull(entryFileDao.getFile(fileId)) {
                "File $fileId not found"
            }
            requireEntry(file.entryId)
            val now = mobileUpdatedAt(file.updatedAt)
            entryFileDao.upsert(
                file.copy(
                    updatedAt = now,
                    deletedAt = now,
                )
            )
            file.localPath
        }
        File(localPath).delete()
    }

    /**
     * Создаёт напоминание эксперимента с новой UTC-версией синхронизации.
     *
     * @param experimentId локальный идентификатор эксперимента.
     * @param text текст напоминания.
     * @param reminderDateTime пользовательские дата и время с локальной семантикой.
     * @return созданное напоминание либо ошибка записи.
     */
    suspend fun createReminder(
        experimentId: Int,
        text: String,
        reminderDateTime: LocalDateTime,
    ): Result<MobileExperimentReminder> = runCatching {
        db.withTransaction {
            val now = mobileUpdatedAt()
            val reminder = MobileExperimentReminderEntity(
                experimentId = experimentId,
                text = text,
                reminderDateTime = reminderDateTime,
                syncId = UUID.randomUUID().toString(),
                updatedAt = now,
            )
            val id = reminderDao.create(reminder).toInt()
            reminder.copy(id = id).toModel()
        }
    }

    /**
     * Обновляет напоминание и повышает его версию независимо от эксперимента.
     *
     * @param reminderId локальный идентификатор напоминания.
     * @param text новый текст.
     * @param reminderDateTime новые дата и время показа.
     * @return успех или ошибка поиска и записи строки.
     */
    suspend fun updateReminder(
        reminderId: Int,
        text: String,
        reminderDateTime: LocalDateTime,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val reminder = requireNotNull(reminderDao.getReminder(reminderId)) {
                "Reminder $reminderId not found"
            }
            val now = mobileUpdatedAt(reminder.updatedAt)
            reminderDao.upsert(
                reminder.copy(
                    text = text,
                    reminderDateTime = reminderDateTime,
                    updatedAt = now,
                )
            )
        }
    }

    /**
     * Помечает напоминание tombstone с версией новее текущей.
     *
     * @param reminderId локальный идентификатор напоминания.
     * @return успех или ошибка поиска и записи строки.
     */
    suspend fun deleteReminder(reminderId: Int): Result<Unit> = runCatching {
        db.withTransaction {
            val reminder = requireNotNull(reminderDao.getReminder(reminderId)) {
                "Reminder $reminderId not found"
            }
            val now = mobileUpdatedAt(reminder.updatedAt)
            reminderDao.upsert(
                reminder.copy(
                    updatedAt = now,
                    deletedAt = now,
                )
            )
        }
    }

    private suspend fun requireExperiment(id: Int): MobileExperimentEntity {
        return requireNotNull(experimentDao.getExperiment(id)) {
            "Experiment $id not found"
        }
    }

    private suspend fun requireEntry(id: Int): MobileExperimentEntryEntity {
        return requireNotNull(entryDao.getEntry(id)) {
            "Entry $id not found"
        }
    }

}

private fun String.toSafeFileName(): String {
    val safeName = trim()
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim('.', ' ')
    return safeName.ifBlank { "file" }
}

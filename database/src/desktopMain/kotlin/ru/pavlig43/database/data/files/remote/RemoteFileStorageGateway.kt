package ru.pavlig43.database.data.files.remote

/**
 * Контракт удалённого объектного хранилища для вложений.
 *
 * Этот слой намеренно изолирован от UI и от конкретного провайдера. Фича файлов знает только:
 * - можно ли сейчас работать с удалённым хранилищем;
 * - как загрузить локальный файл по заданному ключу объекта;
 * - как удалить объект из бакета, если вложение удалено пользователем.
 *
 * За пределами этого интерфейса могут находиться любые реализации:
 * `Yandex Object Storage`, `AWS S3`, `MinIO` или заглушка для локального режима.
 */
interface RemoteFileStorageGateway {
    /**
     * Короткий идентификатор провайдера, который сохраняется в метаданных файла.
     *
     * Нужен для диагностики и для будущих миграций, если в проекте появится больше одного хранилища.
     */
    val providerId: String

    /**
     * Признак того, что реализация действительно настроена и может выполнять сетевые операции.
     *
     * Если `false`, вызывающий код должен работать в локальном режиме без ошибок конфигурации.
     */
    fun isConfigured(): Boolean

    /**
     * Загружает локальный файл в удалённое объектное хранилище.
     *
     * [objectKey] - логический путь объекта внутри бакета.
     * [localPath] - абсолютный путь к локальной копии файла на диске пользователя.
     */
    suspend fun upload(
        objectKey: String,
        localPath: String,
    ): Result<RemoteFileRef>

    /**
     * Скачивает объект из удалённого хранилища в локальный путь.
     */
    suspend fun download(
        objectKey: String,
        localPath: String,
    ): Result<Unit>

    /**
     * Возвращает ключи объектов, которые реально существуют в удалённом хранилище.
     *
     * Нужен для диагностических сценариев вроде поиска объектов без активной строки
     * в бакете.
     */
    suspend fun listObjects(): Result<List<RemoteStorageObject>>

    /**
     * Приводит ключ к формату, который хранится в метаданных.
     */
    fun normalizeObjectKey(objectKey: String): String = objectKey.trimStart('/')

    /**
     * Удаляет объект из удалённого хранилища по его ключу.
     */
    suspend fun delete(
        objectKey: String,
    ): Result<Unit>
}

/**
 * Краткое описание удалённой копии файла после успешной загрузки.
 */
data class RemoteFileRef(
    val providerId: String,
    val objectKey: String,
)

data class RemoteStorageObject(
    val objectKey: String,
    val sizeBytes: Long? = null,
)

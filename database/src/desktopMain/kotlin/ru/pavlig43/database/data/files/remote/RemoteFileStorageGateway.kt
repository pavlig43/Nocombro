package ru.pavlig43.database.data.files.remote

/**
 * Контракт удаленного object storage для вложений.
 *
 * Этот слой намеренно изолирован от UI и от конкретного провайдера. Фича файлов знает только:
 * - можно ли сейчас работать с remote storage;
 * - как загрузить локальный файл по заданному object key;
 * - как удалить объект из bucket, если вложение удалено пользователем.
 *
 * За пределами этого интерфейса могут находиться любые реализации:
 * `Yandex Object Storage`, `AWS S3`, `MinIO` или заглушка для локального режима.
 */
interface RemoteFileStorageGateway {
    /**
     * Короткий идентификатор провайдера, который сохраняется в метаданных файла.
     *
     * Нужен для диагностики и для будущих миграций, если в проекте появится больше одного backend-а.
     */
    val providerId: String

    /**
     * Признак того, что реализация действительно настроена и может выполнять сетевые операции.
     *
     * Если `false`, вызывающий код должен работать в локальном режиме без ошибок конфигурации.
     */
    fun isConfigured(): Boolean

    /**
     * Загружает локальный файл в удаленное object storage.
     *
     * [objectKey] - логический путь объекта внутри bucket.
     * [localPath] - абсолютный путь к локальной копии файла на диске пользователя.
     */
    suspend fun upload(
        objectKey: String,
        localPath: String,
    ): Result<RemoteFileRef>

    /**
     * Скачивает объект из удалённого storage в локальный путь.
     */
    suspend fun download(
        objectKey: String,
        localPath: String,
    ): Result<Unit>

    /**
     * Удаляет объект из удаленного storage по его object key.
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

package ru.pavlig43.database.data.files.remote

/**
 * Заглушка для режима, когда удалённое хранилище не настроено.
 *
 * Она позволяет безопасно поднять приложение без обязательного bucket-конфига:
 * фича файлов продолжает работать локально, а вызовы удалённого хранилища явно сообщают,
 * что удалённый доступ сейчас недоступен по конфигу.
 */
class NoopRemoteFileStorageGateway : RemoteFileStorageGateway {
    override val providerId: String = "noop"

    override fun isConfigured(): Boolean = false

    override suspend fun upload(
        objectKey: String,
        localPath: String,
    ): Result<RemoteFileRef> {
        return Result.failure(
            IllegalStateException("Remote file storage is not configured.")
        )
    }

    override suspend fun download(
        objectKey: String,
        localPath: String,
    ): Result<Unit> {
        return Result.failure(
            IllegalStateException("Remote file storage is not configured.")
        )
    }

    override suspend fun listObjects(): Result<List<RemoteStorageObject>> {
        return Result.failure(
            IllegalStateException("Remote file storage is not configured.")
        )
    }

    override suspend fun delete(
        objectKey: String,
    ): Result<Unit> {
        return Result.failure(
            IllegalStateException("Remote file storage is not configured.")
        )
    }
}

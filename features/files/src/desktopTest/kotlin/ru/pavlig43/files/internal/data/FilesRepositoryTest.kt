package ru.pavlig43.files.internal.data

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.remote.RemoteFileRef
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteStorageObject
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase

/** Проверяет tombstone-удаление метаданных без раннего удаления бинарного объекта из S3. */
class FilesRepositoryTest : DesktopMainDispatcherFunSpec({

    test("user deletion stores a tombstone and leaves S3 cleanup to Doctor") {
        val managed = createSeededManagedTestDatabase()
        val localFile = File.createTempFile("nocombro-user-delete", ".bin")
        try {
            val vendor = managed.database.vendorDao.getAll().first()
            val source = FileBD(
                ownerId = vendor.id,
                ownerFileType = OwnerType.VENDOR,
                displayName = localFile.name,
                path = localFile.absolutePath,
                remoteObjectKey = "files/vendor/${localFile.name}",
                remoteStorageProvider = "fake",
            )
            managed.database.fileDao.upsertFiles(listOf(source))
            val stored = managed.database.fileDao.getFileBySyncId(source.syncId)!!
            val storage = RecordingRemoteFileStorageGateway()

            FilesRepository(managed.database, storage)
                .update(ChangeSet(old = listOf(stored), new = emptyList()))
                .getOrThrow()

            managed.database.fileDao.getFileBySyncId(source.syncId)
                .shouldNotBeNull()
                .deletedAt
                .shouldNotBeNull()
            storage.deletedKeys.shouldContainExactly(emptyList())
            localFile.exists() shouldBe true
            managed.database.mirrorDeletionJournalDao.getAll()
                .filter { it.syncId == source.syncId } shouldBe emptyList()
        } finally {
            localFile.delete()
            managed.close()
        }
    }
})

private class RecordingRemoteFileStorageGateway : RemoteFileStorageGateway {
    override val providerId = "fake"
    val deletedKeys = mutableListOf<String>()

    override fun isConfigured() = true

    override suspend fun upload(objectKey: String, localPath: String) =
        Result.success(RemoteFileRef(providerId, objectKey))

    override suspend fun download(objectKey: String, localPath: String) = Result.success(Unit)

    override suspend fun listObjects() = Result.success(emptyList<RemoteStorageObject>())

    override suspend fun delete(objectKey: String): Result<Unit> {
        deletedKeys += objectKey
        return Result.success(Unit)
    }
}

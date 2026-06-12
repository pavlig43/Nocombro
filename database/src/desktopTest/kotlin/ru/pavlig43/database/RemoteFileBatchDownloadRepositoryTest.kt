package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import java.nio.file.Files
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileRef
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteStorageObject
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase

class RemoteFileBatchDownloadRepositoryTest : DesktopMainDispatcherFunSpec({

    test("missing binary is downloaded after synced file metadata exists locally") {
        val managed = createSeededManagedTestDatabase()
        val target = Files.createTempDirectory("nocombro-mirror-file")
            .resolve("restored.bin")
            .toFile()
        try {
            val vendor = managed.database.vendorDao.getAll().first()
            managed.database.fileDao.upsertFiles(
                listOf(
                    FileBD(
                        ownerId = vendor.id,
                        ownerFileType = OwnerType.VENDOR,
                        displayName = "restored.bin",
                        path = target.absolutePath,
                        remoteObjectKey = "files/restored.bin",
                        remoteStorageProvider = "fake",
                    )
                )
            )
            val gateway = WritingRemoteFileStorageGateway()

            val summary = RemoteFileBatchDownloadRepository(managed.database, gateway)
                .downloadMissingLocalCopies()
                .getOrThrow()

            summary.scannedCount shouldBe 1
            summary.downloadedCount shouldBe 1
            target.readText() shouldBe "mirror-file"
        } finally {
            target.parentFile.deleteRecursively()
            managed.close()
        }
    }
})

private class WritingRemoteFileStorageGateway : RemoteFileStorageGateway {
    override val providerId: String = "fake"

    override fun isConfigured(): Boolean = true

    override suspend fun upload(objectKey: String, localPath: String) =
        Result.success(RemoteFileRef(providerId, objectKey))

    override suspend fun download(objectKey: String, localPath: String): Result<Unit> {
        return runCatching {
            val file = java.io.File(localPath)
            file.parentFile?.mkdirs()
            file.writeText("mirror-file")
        }
    }

    override suspend fun listObjects() =
        Result.success(emptyList<RemoteStorageObject>())

    override suspend fun delete(objectKey: String) =
        Result.success(Unit)
}

package ru.pavlig43.database

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.remote.RemoteFileBatchUploadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileRef
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteStorageObject
import ru.pavlig43.database.data.sync.mirror.FileMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import java.nio.file.Files

class RemoteFileBatchUploadRepositoryTest : DesktopMainDispatcherFunSpec({

    test("uploads active local files in input order") {
        val first = Files.createTempFile("nocombro-upload-first", ".bin").toFile()
        val second = Files.createTempFile("nocombro-upload-second", ".bin").toFile()
        try {
            first.writeText("first")
            second.writeText("second")
            val gateway = RecordingFileStorageGateway()
            val repository = RemoteFileBatchUploadRepository(gateway)

            val result = repository.uploadLocalWinners(
                listOf(
                    fileChange("first", first.absolutePath, "files/first.bin"),
                    fileChange("second", second.absolutePath, "files/second.bin"),
                )
            ).getOrThrow()

            result shouldBe 2
            gateway.uploadedKeys.shouldContainExactly("files/first.bin", "files/second.bin")
        } finally {
            first.delete()
            second.delete()
        }
    }

    test("skips tombstones and rows without a remote key without requiring S3") {
        val repository = RemoteFileBatchUploadRepository(
            RecordingFileStorageGateway(configured = false)
        )

        val result = repository.uploadLocalWinners(
            listOf(
                fileChange(
                    syncId = "deleted",
                    path = "missing-deleted.bin",
                    remoteObjectKey = "files/deleted.bin",
                    deletedAt = LocalDateTime(2026, 7, 1, 10, 0),
                ),
                fileChange(
                    syncId = "legacy",
                    path = "missing-legacy.bin",
                    remoteObjectKey = null,
                ),
            )
        ).getOrThrow()

        result shouldBe 0
    }

    test("fails before upload when the local file is missing") {
        val gateway = RecordingFileStorageGateway()
        val repository = RemoteFileBatchUploadRepository(gateway)

        val result = repository.uploadLocalWinners(
            listOf(fileChange("missing", "definitely-missing.bin", "files/missing.bin"))
        )

        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe
            "Local file is missing: table=file, sync_id=missing"
        gateway.uploadedKeys shouldBe emptyList()
    }

    test("stops the batch on the first S3 error") {
        val first = Files.createTempFile("nocombro-upload-ok", ".bin").toFile()
        val second = Files.createTempFile("nocombro-upload-fail", ".bin").toFile()
        try {
            val gateway = RecordingFileStorageGateway(failingKey = "files/fail.bin")
            val repository = RemoteFileBatchUploadRepository(gateway)

            val result = repository.uploadLocalWinners(
                listOf(
                    fileChange("ok", first.absolutePath, "files/ok.bin"),
                    fileChange("fail", second.absolutePath, "files/fail.bin"),
                )
            )

            result.isFailure shouldBe true
            gateway.uploadedKeys.shouldContainExactly("files/ok.bin", "files/fail.bin")
        } finally {
            first.delete()
            second.delete()
        }
    }
})

private fun fileChange(
    syncId: String,
    path: String,
    remoteObjectKey: String?,
    deletedAt: LocalDateTime? = null,
): MirrorPushEntityChange {
    return MirrorPushEntityChange(
        table = MirrorSyncTable.FILE,
        row = FileMirrorRow(
            syncId = syncId,
            ownerType = OwnerType.PRODUCT,
            ownerSyncId = "owner",
            displayName = "file.bin",
            path = path,
            remoteObjectKey = remoteObjectKey,
            remoteStorageProvider = "fake",
            updatedAt = LocalDateTime(2026, 7, 1, 9, 0),
            deletedAt = deletedAt,
        ),
    )
}

private class RecordingFileStorageGateway(
    private val configured: Boolean = true,
    private val failingKey: String? = null,
) : RemoteFileStorageGateway {
    val uploadedKeys = mutableListOf<String>()
    override val providerId: String = "fake"

    override fun isConfigured(): Boolean = configured

    override suspend fun upload(objectKey: String, localPath: String): Result<RemoteFileRef> {
        uploadedKeys += objectKey
        return if (objectKey == failingKey) {
            Result.failure(IllegalStateException("S3 unavailable"))
        } else {
            Result.success(RemoteFileRef(providerId, objectKey))
        }
    }

    override suspend fun download(objectKey: String, localPath: String) = Result.success(Unit)
    override suspend fun listObjects() = Result.success(emptyList<RemoteStorageObject>())
    override suspend fun delete(objectKey: String) = Result.success(Unit)
}

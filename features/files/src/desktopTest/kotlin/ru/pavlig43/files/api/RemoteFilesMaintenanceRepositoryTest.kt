package ru.pavlig43.files.api

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.remote.RemoteFileRef
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteStorageObject
import ru.pavlig43.database.data.sync.mirror.FileMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorPullRequest
import ru.pavlig43.database.data.sync.mirror.MirrorPullResult
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorPushResult
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteStatus
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRemoteGateway
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase

class RemoteFilesMaintenanceRepositoryTest : DesktopMainDispatcherFunSpec({

    test("active mirror file keys are protected while tombstones and blank keys are not") {
        withEmptyTestDatabase { db ->
            val storage = FakeStorage(setOf("active", "deleted", "blank", "missing"))
            val mirror = FakeMirrorGateway(
                rows = listOf(
                    fileRow("active", "active"),
                    fileRow("deleted", "deleted", deleted = true),
                    fileRow("blank", ""),
                )
            )
            val repository = RemoteFilesMaintenanceRepository(db, storage, mirror)

            repository.getOrphanRemoteFiles().getOrThrow()
                .map { it.objectKey }
                .shouldContainExactly("blank", "deleted", "missing")
        }
    }

    test("delete reloads mirror and preserves a key that became active") {
        withEmptyTestDatabase { db ->
            val storage = FakeStorage(setOf("candidate"))
            val mirror = FakeMirrorGateway(emptyList())
            val repository = RemoteFilesMaintenanceRepository(db, storage, mirror)

            repository.getOrphanRemoteFiles().getOrThrow()
                .map { it.objectKey } shouldContainExactly listOf("candidate")

            mirror.rows = listOf(fileRow("candidate", "candidate"))

            repository.deleteRemoteFile("candidate").getOrThrow()
            storage.deletedKeys shouldBe emptyList()
        }
    }

    test("prefixed mirror keys protect logical S3 objects") {
        withEmptyTestDatabase { db ->
            val storage = FakeStorage(setOf("files/product/spec.pdf"))
            val mirror = FakeMirrorGateway(
                rows = listOf(
                    fileRow("spec", "nocombro/files/product/spec.pdf"),
                )
            )
            val repository = RemoteFilesMaintenanceRepository(db, storage, mirror)

            repository.getOrphanRemoteFiles().getOrThrow()
                .map { it.objectKey } shouldBe emptyList()
        }
    }
})

private val testTime = LocalDateTime(2026, 6, 12, 12, 0)

private fun fileRow(
    syncId: String,
    remoteObjectKey: String?,
    deleted: Boolean = false,
) = FileMirrorRow(
    syncId = syncId,
    ownerType = OwnerType.PRODUCT,
    ownerSyncId = "owner",
    displayName = syncId,
    path = syncId,
    remoteObjectKey = remoteObjectKey,
    updatedAt = testTime,
    deletedAt = testTime.takeIf { deleted },
)

private class FakeStorage(
    private val keys: Set<String>,
) : RemoteFileStorageGateway {
    override val providerId = "fake"
    val deletedKeys = mutableListOf<String>()

    override fun isConfigured() = true
    override suspend fun upload(objectKey: String, localPath: String) =
        Result.success(RemoteFileRef(providerId, objectKey))
    override suspend fun download(objectKey: String, localPath: String) = Result.success(Unit)
    override suspend fun listObjects() =
        Result.success(keys.map { RemoteStorageObject(it) })
    override fun normalizeObjectKey(objectKey: String) =
        objectKey.removePrefix("nocombro/")
    override suspend fun delete(objectKey: String): Result<Unit> {
        deletedKeys += objectKey
        return Result.success(Unit)
    }
}

private class FakeMirrorGateway(
    var rows: List<FileMirrorRow>,
) : MirrorSyncRemoteGateway {
    override suspend fun getStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = setOf(MirrorSyncTable.FILE.tableName),
        checkedAt = testTime,
    )

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>) =
        Result.success(
            MirrorRemoteSnapshot(
                loadedAt = testTime,
                rowsByTable = mapOf(MirrorSyncTable.FILE to rows),
            )
        )

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>) =
        Result.success(MirrorPushResult(testTime, emptySet()))

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.success(MirrorPullResult(testTime, emptyList()))
}

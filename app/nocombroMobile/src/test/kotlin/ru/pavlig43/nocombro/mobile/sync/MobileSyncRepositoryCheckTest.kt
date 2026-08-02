package ru.pavlig43.nocombro.mobile.sync

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MobileSyncRepositoryCheckTest {
    @Test
    fun `check reads each side once and builds status and preview from one plan`() = runTest {
        val local = snapshot(
            experiment(
                syncId = "local-experiment",
                title = "К отправке",
                updatedAt = "2026-08-02T10:00:00",
            )
        )
        val remote = snapshot(
            experiment(
                syncId = "remote-experiment",
                title = "К получению",
                updatedAt = "2026-08-02T11:00:00",
            ),
            loadedAt = "2026-08-02T12:00:00",
        )
        val localSource = CountingLocalSource(local)
        val remoteGateway = CountingRemoteGateway(remote)
        val repository = MobileSyncRepository(
            configRepository = FakeConfigSource,
            localRepository = localSource,
            remoteGatewayFactory = { _, _ -> remoteGateway },
            storageGatewayFactory = { NoOpStorageGateway },
        )

        val result = repository.check()
        val preview = assertNotNull(result.preview)

        assertEquals(1, localSource.loadCount)
        assertEquals(1, remoteGateway.loadCount)
        assertEquals(1, result.status.localChanges)
        assertEquals(1, result.status.remoteChanges)
        assertEquals(remote.loadedAt, result.status.checkedAt)
        assertEquals(remote.loadedAt, preview.snapshotAt)
        assertEquals("К отправке", preview.localChanges.single().title)
        assertEquals("К получению", preview.remoteChanges.single().title)
    }
}

private class CountingLocalSource(
    private val snapshot: MobileMirrorSnapshot,
) : MobileLocalMirrorDataSource {
    var loadCount: Int = 0

    override suspend fun loadSnapshot(config: MobileS3Config): MobileMirrorSnapshot {
        loadCount += 1
        return snapshot
    }

    override suspend fun applyRemoteChanges(
        changes: List<MobileMirrorChange>,
        config: MobileS3Config,
    ) = Unit
}

private class CountingRemoteGateway(
    private val snapshot: MobileMirrorSnapshot,
) : MobileRemoteMirrorGateway {
    var loadCount: Int = 0

    override fun loadSnapshot(): Result<MobileMirrorSnapshot> {
        loadCount += 1
        return Result.success(snapshot)
    }

    override fun push(changes: List<MobileMirrorChange>): Result<MobilePushResult> {
        error("Push must not run during check")
    }
}

private object FakeConfigSource : MobileRemoteConfigSource {
    override fun load(): Result<MobileRemoteConfig> = Result.success(TEST_CONFIG)

    override fun decodeServiceAccountJson(config: MobileYdbConfig): String? = null
}

private object NoOpStorageGateway : MobileObjectStorageGateway {
    override suspend fun uploadFile(localPath: String, remoteKey: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun downloadFile(remoteKey: String, localPath: String): Result<Unit> =
        Result.success(Unit)
}

private fun snapshot(
    vararg rows: MobileMirrorRow,
    loadedAt: String = "2026-08-02T09:00:00",
): MobileMirrorSnapshot = MobileMirrorSnapshot(
    loadedAt = LocalDateTime.parse(loadedAt),
    rowsByTable = MobileMirrorTable.entries.associateWith { table ->
        rows.filter { row ->
            table == when (row) {
                is MobileExperimentMirrorRow -> MobileMirrorTable.EXPERIMENT
                is MobileExperimentEntryMirrorRow -> MobileMirrorTable.EXPERIMENT_ENTRY
                is MobileExperimentReminderMirrorRow -> MobileMirrorTable.EXPERIMENT_REMINDER
                is MobileFileMirrorRow -> MobileMirrorTable.FILE
            }
        }
    },
)

private fun experiment(
    syncId: String,
    title: String,
    updatedAt: String,
) = MobileExperimentMirrorRow(
    syncId = syncId,
    title = title,
    ideaDescription = "Описание",
    isArchived = false,
    updatedAt = LocalDateTime.parse(updatedAt),
    deletedAt = null,
)

private val TEST_CONFIG = MobileRemoteConfig(
    ydb = MobileYdbConfig(
        jdbcUrl = "jdbc:ydb:test",
        token = "test-token",
    ),
    s3 = MobileS3Config(
        endpoint = "https://s3.test",
        bucket = "test",
        region = "test",
        accessKeyId = "test",
        secretAccessKey = "test",
    ),
)

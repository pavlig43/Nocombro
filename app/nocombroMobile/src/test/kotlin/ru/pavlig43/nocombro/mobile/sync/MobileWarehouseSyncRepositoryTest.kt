package ru.pavlig43.nocombro.mobile.sync

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.nocombro.mobile.warehouse.MobileWarehouseProduct
import ru.pavlig43.nocombro.mobile.warehouse.MobileWarehouseRemoteDataSource
import ru.pavlig43.nocombro.mobile.warehouse.MobileWarehouseSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MobileWarehouseSyncRepositoryTest {
    @Test
    fun `check and push never load or store warehouse snapshot`() = runTest {
        val fixture = WarehouseSyncFixture()

        fixture.repository.check()
        fixture.repository.push()

        assertEquals(0, fixture.warehouseRemote.loadCount)
        assertEquals(0, fixture.local.atomicApplyCount)
        assertTrue(fixture.remote.pushedChanges.flatten().none { it.table.name.contains("WAREHOUSE") })
        assertTrue(MobileMirrorTable.entries.none { it.name.contains("WAREHOUSE") })
    }

    @Test
    fun `pull and full sync replace warehouse through atomic local apply`() = runTest {
        val pullFixture = WarehouseSyncFixture()
        val syncFixture = WarehouseSyncFixture()

        val pullResult = pullFixture.repository.pull()
        val syncResult = syncFixture.repository.sync()

        assertNull(pullResult.error)
        assertNull(syncResult.error)
        assertEquals(1, pullFixture.warehouseRemote.loadCount)
        assertEquals(1, pullFixture.local.atomicApplyCount)
        assertEquals(pullFixture.warehouseRemote.snapshot, pullFixture.local.warehouseSnapshot)
        assertEquals(1, syncFixture.warehouseRemote.loadCount)
        assertEquals(1, syncFixture.local.atomicApplyCount)
        assertEquals(syncFixture.warehouseRemote.snapshot, syncFixture.local.warehouseSnapshot)
    }

    @Test
    fun `warehouse loading error preserves previous local snapshot`() = runTest {
        val fixture = WarehouseSyncFixture(warehouseFailure = IllegalStateException("bad relation"))
        val previous = fixture.local.warehouseSnapshot

        val result = fixture.repository.pull()

        assertEquals(1, fixture.warehouseRemote.loadCount)
        assertEquals(0, fixture.local.atomicApplyCount)
        assertEquals(previous, fixture.local.warehouseSnapshot)
        assertTrue(result.error.orEmpty().contains("снимок склада"))
    }
}

private class WarehouseSyncFixture(
    warehouseFailure: Throwable? = null,
) {
    val local = WarehouseRecordingLocalSource()
    val remote = WarehouseRecordingMirrorGateway()
    val warehouseRemote = WarehouseRecordingRemoteSource(warehouseFailure)
    val repository = MobileSyncRepository(
        configRepository = WarehouseTestConfigSource,
        localRepository = local,
        remoteGatewayFactory = { _, _ -> remote },
        warehouseRemoteDataSourceFactory = { _, _ -> warehouseRemote },
        storageGatewayFactory = { WarehouseNoOpStorageGateway },
    )
}

private class WarehouseRecordingLocalSource : MobileLocalMirrorDataSource {
    var atomicApplyCount = 0
    var warehouseSnapshot: MobileWarehouseSnapshot? = MobileWarehouseSnapshot(
        updatedAt = LocalDateTime.parse("2026-08-01T10:00:00"),
        products = listOf(MobileWarehouseProduct("old", "Старый", 1, 2)),
    )

    override suspend fun loadSnapshot(config: MobileS3Config): MobileMirrorSnapshot = emptyMirrorSnapshot()

    override suspend fun applyRemoteChanges(
        changes: List<MobileMirrorChange>,
        config: MobileS3Config,
    ) = Unit

    override suspend fun applyRemoteChangesAndWarehouseSnapshot(
        changes: List<MobileMirrorChange>,
        config: MobileS3Config,
        warehouseSnapshot: MobileWarehouseSnapshot,
    ) {
        atomicApplyCount += 1
        this.warehouseSnapshot = warehouseSnapshot
    }
}

private class WarehouseRecordingMirrorGateway : MobileRemoteMirrorGateway {
    val pushedChanges = mutableListOf<List<MobileMirrorChange>>()

    override fun loadSnapshot(): Result<MobileMirrorSnapshot> = Result.success(emptyMirrorSnapshot())

    override fun push(changes: List<MobileMirrorChange>): Result<MobilePushResult> {
        pushedChanges += changes
        return Result.success(MobilePushResult(changes, emptyList()))
    }
}

private class WarehouseRecordingRemoteSource(
    private val failure: Throwable?,
) : MobileWarehouseRemoteDataSource {
    var loadCount = 0
    val snapshot = MobileWarehouseSnapshot(
        updatedAt = LocalDateTime.parse("2026-08-06T14:32:00"),
        products = listOf(MobileWarehouseProduct("new", "Новый", 3, 4)),
    )

    override fun loadSnapshot(): Result<MobileWarehouseSnapshot> {
        loadCount += 1
        return failure?.let(Result.Companion::failure) ?: Result.success(snapshot)
    }
}

private object WarehouseTestConfigSource : MobileRemoteConfigSource {
    override fun load(): Result<MobileRemoteConfig> = Result.success(
        MobileRemoteConfig(
            ydb = MobileYdbConfig(jdbcUrl = "jdbc:ydb:test", token = "test-token"),
            s3 = MobileS3Config(
                endpoint = "https://s3.test",
                bucket = "test",
                region = "test",
                accessKeyId = "test",
                secretAccessKey = "test",
            ),
        )
    )

    override fun decodeServiceAccountJson(config: MobileYdbConfig): String? = null
}

private object WarehouseNoOpStorageGateway : MobileObjectStorageGateway {
    override suspend fun uploadFile(localPath: String, remoteKey: String): Result<Unit> = Result.success(Unit)

    override suspend fun downloadFile(remoteKey: String, localPath: String): Result<Unit> = Result.success(Unit)
}

private fun emptyMirrorSnapshot() = MobileMirrorSnapshot(
    loadedAt = LocalDateTime.parse("2026-08-06T14:32:00"),
    rowsByTable = MobileMirrorTable.entries.associateWith { emptyList() },
)

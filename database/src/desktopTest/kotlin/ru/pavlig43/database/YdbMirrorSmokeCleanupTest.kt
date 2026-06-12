package ru.pavlig43.database

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig

class YdbMirrorSmokeCleanupTest : FunSpec({

    val cleanupEnabled = System.getenv("NOCOMBRO_YDB_CLEANUP_SMOKE")
        ?.equals("true", ignoreCase = true)
        ?: false

    test("remove only Codex smoke rows from real YDB mirror")
        .config(enabled = cleanupEnabled) {
            val gateway = YdbJdbcMirrorSyncGateway(
                requireNotNull(YdbMirrorJdbcConfig.fromEnvironment())
            )
            val tables = MirrorSyncTable.mirroredBusinessTables
            val before = gateway.loadRemoteSnapshot(tables).getOrThrow()
            val smokeIdsByTable = tables.associateWith { table ->
                before.rowsByTable[table].orEmpty()
                    .map { it.syncId }
                    .filter(::isSmokeSyncId)
            }.filterValues(List<String>::isNotEmpty)

            val expectedDeleted = smokeIdsByTable.values.sumOf(List<String>::size)
            gateway.deleteRows(smokeIdsByTable).getOrThrow() shouldBe expectedDeleted

            val after = gateway.loadRemoteSnapshot(tables).getOrThrow()
            after.rowsByTable.values.flatten().count { isSmokeSyncId(it.syncId) } shouldBe 0
            println("MIRROR_SMOKE_CLEANUP deleted=$expectedDeleted")
        }
})

private fun isSmokeSyncId(syncId: String): Boolean {
    return syncId.startsWith("codex-smoke-") ||
        syncId.startsWith("codex-device-smoke-") ||
        syncId in knownLeakedTestSyncIds
}

private val knownLeakedTestSyncIds = setOf(
    "aabaf1a1-735e-4dad-be9d-8394a9a3d2d4",
)

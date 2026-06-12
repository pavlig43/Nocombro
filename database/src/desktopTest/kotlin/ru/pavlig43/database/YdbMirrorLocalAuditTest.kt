package ru.pavlig43.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRow
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import java.io.File

class YdbMirrorLocalAuditTest : FunSpec({

    val auditEnabled = System.getenv("NOCOMBRO_YDB_LOCAL_AUDIT")
        ?.equals("true", ignoreCase = true)
        ?: false

    test("working local database exactly matches business rows in YDB mirror")
        .config(enabled = auditEnabled) {
            val config = requireNotNull(YdbMirrorJdbcConfig.fromEnvironment())
            val appData = requireNotNull(System.getenv("APPDATA"))
            val databaseFile = File(appData, "Nocombro/nocombro.db")
            check(databaseFile.exists()) { "Working database not found: $databaseFile" }

            val db = Room.databaseBuilder<NocombroDatabase>(databaseFile.absolutePath)
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            try {
                val tables = MirrorSyncTable.mirroredBusinessTables
                val local = MirrorLocalSnapshotRepository(db).loadSnapshot(tables)
                val remote = YdbJdbcMirrorSyncGateway(config)
                    .loadRemoteSnapshot(tables)
                    .getOrThrow()
                var businessDifferences = 0
                var smokeRows = 0

                tables.forEach { table ->
                    val localRows = local.rowsByTable[table].orEmpty()
                        .associateBy(MirrorSyncRow::syncId)
                    val remoteRows = remote.rowsByTable[table].orEmpty()
                        .associateBy(MirrorSyncRow::syncId)
                    val ids = localRows.keys + remoteRows.keys
                    val smokeIds = ids.count(::isCodexSmokeId)
                    val differentIds = ids
                        .asSequence()
                        .filterNot(::isCodexSmokeId)
                        .filter { syncId -> localRows[syncId] != remoteRows[syncId] }
                        .toList()

                    smokeRows += smokeIds
                    businessDifferences += differentIds.size
                    println(
                        "MIRROR_AUDIT table=${table.tableName} " +
                            "local=${localRows.size} remote=${remoteRows.size} " +
                            "businessDiff=${differentIds.size} smoke=$smokeIds"
                    )
                    differentIds.take(10).forEach { syncId ->
                        println(
                            "MIRROR_AUDIT_DIFF table=${table.tableName} syncId=$syncId " +
                                "local=${localRows[syncId] != null} remote=${remoteRows[syncId] != null}"
                        )
                    }
                }

                println(
                    "MIRROR_AUDIT_TOTAL businessDiff=$businessDifferences smokeRows=$smokeRows"
                )
                businessDifferences shouldBe 0
            } finally {
                db.close()
            }
        }
})

private fun isCodexSmokeId(syncId: String): Boolean {
    return syncId.startsWith("codex-smoke-") ||
        syncId.startsWith("codex-device-smoke-")
}

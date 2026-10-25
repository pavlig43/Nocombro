package ru.pavlig43.database.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.pavlig43.database.*
import ru.pavlig43.database.data.sync.mirror.*
import java.io.File

/** Служебная точка входа: только снимки и отчёты, без UI, применения плана и S3. */
fun main(args: Array<String>) = runBlocking {
    require(args.size == 2) { "Expected SQLite backup path and report directory" }
    val backup = File(args[0]).canonicalFile
    require(backup.isFile) { "SQLite backup does not exist" }
    val original = File(System.getenv("APPDATA"), "Nocombro/nocombro.db").canonicalFile
    require(backup != original) { "Use a SQLite backup, never the original database" }
    val db = Room.databaseBuilder<NocombroDatabase>(name = backup.path)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    try {
        val local = MirrorLocalSnapshotRepository(db).loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val gateway = YdbJdbcMirrorSyncGateway(YdbMirrorJdbcConfig.fromEnvironment())
        val remote = gateway.loadRemoteSnapshot().getOrThrow()
        val plan = MirrorReconciliationPlanner().plan(local, remote)
        val writer = SyncAnalysisReportWriter(reportDirectory = { File(args[1]) })
        writer.write(MirrorReconciliationPreview(local, remote, plan))
        println("Room snapshot: ${local.loadedAt}; YDB snapshot: ${remote.loadedAt}")
        println("Push: ${plan.pushChanges.size}; pull: ${plan.pullChanges.size}; conflicts: ${plan.conflicts.size}; excluded: ${plan.excludedPullChanges.size}")
        println(writer.latestHtmlReport().getOrThrow().absolutePath)
    } finally {
        db.close()
    }
}

package ru.pavlig43.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageConfig
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageGateway
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.FileMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRow
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import ru.pavlig43.testkit.database.createManagedTestDatabase
import java.io.File
import java.nio.file.Files

class YdbMirrorDisasterRecoveryTest : FunSpec({

    val recoveryEnabled = System.getenv("NOCOMBRO_YDB_DISASTER_RECOVERY")
        ?.equals("true", ignoreCase = true)
        ?: false

    test("empty database and missing binaries are restored from YDB and S3")
        .config(enabled = recoveryEnabled) {
            val tables = MirrorSyncTable.mirroredBusinessTables
            val appData = requireNotNull(System.getenv("APPDATA"))
            val workingDatabaseFile = File(appData, "Nocombro/nocombro.db")
            val source = Room.databaseBuilder<NocombroDatabase>(workingDatabaseFile.absolutePath)
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            val target = createManagedTestDatabase()
            val downloadRoot = Files.createTempDirectory("nocombro-recovery-files").toFile()
            try {
                val sourceSnapshot = MirrorLocalSnapshotRepository(source).loadSnapshot(tables)
                val remoteSnapshot = YdbJdbcMirrorSyncGateway(
                    YdbMirrorJdbcConfig.fromEnvironment()
                ).loadRemoteSnapshot(tables).getOrThrow()
                val remoteChanges = tables.flatMap { table ->
                    remoteSnapshot.rowsByTable[table].orEmpty().map { row ->
                        MirrorPushEntityChange(table, row)
                    }
                }

                MirrorLocalApplyRepository(
                    target.database,
                    MirrorEntityApplyRepository(target.database),
                ).apply(remoteChanges)

                val restoredSnapshot = MirrorLocalSnapshotRepository(target.database)
                    .loadSnapshot(tables)
                tables.forEach { table ->
                    restoredSnapshot.rowsByTable[table].orEmpty()
                        .sortedBy(MirrorSyncRow::syncId) shouldBe
                        sourceSnapshot.rowsByTable[table].orEmpty()
                            .sortedBy(MirrorSyncRow::syncId)
                }

                val activeRemoteFiles = remoteSnapshot.rowsByTable[MirrorSyncTable.FILE]
                    .orEmpty()
                    .filterIsInstance<FileMirrorRow>()
                    .filter { it.deletedAt == null }
                    .mapNotNull { row ->
                        row.remoteObjectKey?.takeIf(String::isNotBlank)?.let { key -> row to key }
                    }
                val storageGateway = S3RemoteFileStorageGateway(
                    requireNotNull(S3RemoteFileStorageConfig.fromEnvironment()) {
                        "S3 configuration is required for disaster recovery test"
                    }
                )
                val downloadedKeys = mutableListOf<String>()
                val missingKeys = mutableListOf<String>()
                activeRemoteFiles.forEachIndexed { index, (_, objectKey) ->
                    val targetFile = File(downloadRoot, "$index.bin")
                    val result = storageGateway.download(objectKey, targetFile.absolutePath)
                    if (result.isSuccess) {
                        targetFile.exists() shouldBe true
                        downloadedKeys += objectKey
                    } else {
                        missingKeys += objectKey
                        println("MIRROR_DISASTER_RECOVERY_MISSING key=$objectKey")
                    }
                }

                missingKeys shouldBe emptyList()
                downloadedKeys shouldContainExactlyInAnyOrder
                    activeRemoteFiles.map { (_, objectKey) -> objectKey }
                println(
                    "MIRROR_DISASTER_RECOVERY rows=${remoteChanges.size} " +
                        "files=${downloadedKeys.size}"
                )
            } finally {
                downloadRoot.deleteRecursively()
                target.close()
                source.close()
            }
        }
})

package ru.pavlig43.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageConfig
import ru.pavlig43.database.data.files.remote.S3RemoteFileStorageGateway
import ru.pavlig43.database.data.sync.mirror.FileMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import java.io.File
import java.nio.file.Files

class YdbMirrorFileRepairTest : FunSpec({

    val repairEnabled = System.getenv("NOCOMBRO_YDB_REPAIR_FILES")
        ?.equals("true", ignoreCase = true)
        ?: false

    test("upload local binaries that are missing from S3")
        .config(enabled = repairEnabled) {
            val appData = requireNotNull(System.getenv("APPDATA"))
            val workingDatabaseFile = File(appData, "Nocombro/nocombro.db")
            val db = Room.databaseBuilder<NocombroDatabase>(workingDatabaseFile.absolutePath)
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            val probeRoot = Files.createTempDirectory("nocombro-file-repair").toFile()
            try {
                val gateway = S3RemoteFileStorageGateway(
                    requireNotNull(S3RemoteFileStorageConfig.fromEnvironment())
                )
                val files = MirrorLocalSnapshotRepository(db)
                    .loadSnapshot(listOf(MirrorSyncTable.FILE))
                    .rowsByTable
                    .getValue(MirrorSyncTable.FILE)
                    .filterIsInstance<FileMirrorRow>()
                    .filter { it.deletedAt == null && !it.remoteObjectKey.isNullOrBlank() }
                var repaired = 0
                val unrecoverable = mutableListOf<String>()

                files.forEachIndexed { index, row ->
                    val objectKey = row.remoteObjectKey.orEmpty()
                    val probe = File(probeRoot, "$index.bin")
                    if (gateway.download(objectKey, probe.absolutePath).isSuccess) {
                        return@forEachIndexed
                    }
                    val localFile = File(row.path)
                    if (!localFile.exists()) {
                        unrecoverable += row.syncId
                        return@forEachIndexed
                    }
                    gateway.upload(objectKey, localFile.absolutePath).getOrThrow()
                    repaired += 1
                }

                unrecoverable shouldBe emptyList()
                println("MIRROR_FILE_REPAIR repaired=$repaired checked=${files.size}")
            } finally {
                probeRoot.deleteRecursively()
                db.close()
            }
        }
})

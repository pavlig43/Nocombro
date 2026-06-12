package ru.pavlig43.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlanner
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import java.io.File

class YdbMirrorWorkingDatabaseRebuildTest : FunSpec({

    val rebuildEnabled = System.getenv("NOCOMBRO_YDB_REBUILD_WORKING_DATABASE")
        ?.equals("true", ignoreCase = true)
        ?: false

    test("rebuild real YDB mirror from the working local database")
        .config(enabled = rebuildEnabled) {
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
                val entityApplyRepository = MirrorEntityApplyRepository(db)
                val service = MirrorReconciliationService(
                    localSnapshotRepository = MirrorLocalSnapshotRepository(db),
                    remoteGateway = YdbJdbcMirrorSyncGateway(config),
                    planner = MirrorReconciliationPlanner(),
                    localApplyRepository = MirrorLocalApplyRepository(db, entityApplyRepository),
                )

                val result = service.rebuildRemoteFromLocal().getOrThrow()
                println(
                    "MIRROR_REBUILD pushed=${result.pushedRows} " +
                        "tombstoned=${result.tombstonedRows} at=${result.rebuiltAt}"
                )
                result.pushedRows shouldBeGreaterThan 0
            } finally {
                db.close()
            }
        }
})

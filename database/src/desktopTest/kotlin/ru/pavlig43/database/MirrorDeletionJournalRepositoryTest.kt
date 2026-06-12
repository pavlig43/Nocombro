package ru.pavlig43.database

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.sync.mirror.MirrorDeletionJournalRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.orderedForLocalApply
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase

class MirrorDeletionJournalRepositoryTest : DesktopMainDispatcherFunSpec({

    test("hard deleting a parent journals both parent and cascaded child tombstones") {
        withEmptyTestDatabase { db ->
            val vendor = Vendor(displayName = "Journal vendor")
            val vendorId = db.vendorDao.create(vendor).toInt()
            val declaration = Declaration(
                displayName = "Journal declaration",
                createdAt = LocalDate(2026, 6, 10),
                vendorId = vendorId,
                vendorName = vendor.displayName,
                bornDate = LocalDate(2026, 6, 1),
                bestBefore = LocalDate(2026, 12, 1),
                observeFromNotification = false,
            )
            db.declarationDao.create(declaration)
            db.fileDao.upsertFiles(
                listOf(
                    FileBD(
                        ownerId = vendorId,
                        ownerFileType = OwnerType.VENDOR,
                        displayName = "vendor-file.txt",
                        path = "C:/missing/vendor-file.txt",
                    )
                )
            )

            MirrorDeletionJournalRepository(db).captureHardDeletes {
                db.vendorDao.deleteVendorsByIds(setOf(vendorId))
            }

            db.vendorDao.getAll() shouldContainExactly emptyList()
            db.declarationDao.getAll() shouldContainExactly emptyList()
            db.fileDao.getAllFiles() shouldContainExactly emptyList()

            val snapshot = MirrorLocalSnapshotRepository(db)
                .loadSnapshot(
                    listOf(
                        MirrorSyncTable.VENDOR,
                        MirrorSyncTable.DECLARATION,
                        MirrorSyncTable.FILE,
                    )
                )
            val tombstones = listOf(
                MirrorPushEntityChange(
                    MirrorSyncTable.VENDOR,
                    snapshot.rowsByTable.getValue(MirrorSyncTable.VENDOR).single(),
                ),
                MirrorPushEntityChange(
                    MirrorSyncTable.DECLARATION,
                    snapshot.rowsByTable.getValue(MirrorSyncTable.DECLARATION).single(),
                ),
                MirrorPushEntityChange(
                    MirrorSyncTable.FILE,
                    snapshot.rowsByTable.getValue(MirrorSyncTable.FILE).single(),
                ),
            )

            tombstones.forEach { it.row.deletedAt.shouldNotBeNull() }
            tombstones.orderedForLocalApply().map(MirrorPushEntityChange::table) shouldContainExactly
                listOf(MirrorSyncTable.FILE, MirrorSyncTable.DECLARATION, MirrorSyncTable.VENDOR)

        }
    }
})

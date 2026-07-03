package ru.pavlig43.files.api

import io.kotest.matchers.shouldBe
import java.io.File
import java.util.UUID
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.getManagedFilesRootDirectory
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase

class LocalFilesMaintenanceRepositoryTest : DesktopMainDispatcherFunSpec({

    test("orphan remains on disk until confirmed cleanup deletes it") {
        withEmptyTestDatabase { db ->
            val testDirectory = File(
                getManagedFilesRootDirectory(),
                "doctor-test-${UUID.randomUUID()}",
            )
            val orphan = File(testDirectory, "orphan.bin")
            try {
                testDirectory.mkdirs()
                orphan.writeText("orphan")
                val repository = LocalFilesMaintenanceRepository(db)

                repository.getOrphanLocalFiles().getOrThrow()
                    .any { it.path == orphan.absolutePath } shouldBe true
                orphan.exists() shouldBe true

                repository.deleteLocalFile(orphan.absolutePath).getOrThrow()

                orphan.exists() shouldBe false
            } finally {
                testDirectory.deleteRecursively()
            }
        }
    }

    test("local binary for tombstoned file row is reported as cleanup candidate") {
        withEmptyTestDatabase { db ->
            val testDirectory = File(
                getManagedFilesRootDirectory(),
                "doctor-test-${UUID.randomUUID()}",
            )
            val activeFile = File(testDirectory, "active.bin")
            val deletedFile = File(testDirectory, "deleted.bin")
            try {
                testDirectory.mkdirs()
                activeFile.writeText("active")
                deletedFile.writeText("deleted")
                val deletedAt = LocalDateTime(2026, 7, 3, 20, 18)
                db.fileDao.upsertFiles(
                    listOf(
                        fileRow(path = activeFile.absolutePath),
                        fileRow(path = deletedFile.absolutePath, deletedAt = deletedAt),
                    )
                )
                val repository = LocalFilesMaintenanceRepository(db)

                val orphanPaths = repository.getOrphanLocalFiles().getOrThrow()
                    .map { it.path }

                (activeFile.absolutePath in orphanPaths) shouldBe false
                (deletedFile.absolutePath in orphanPaths) shouldBe true
                (repository.getStorageOverview().getOrThrow().orphanFilesCount > 0) shouldBe true
            } finally {
                testDirectory.deleteRecursively()
            }
        }
    }
})

private fun fileRow(
    path: String,
    deletedAt: LocalDateTime? = null,
) = FileBD(
    ownerId = 1,
    ownerFileType = OwnerType.EXPERIMENT_ENTRY,
    displayName = File(path).name,
    path = path,
    deletedAt = deletedAt,
)

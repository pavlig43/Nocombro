package ru.pavlig43.files.api

import io.kotest.matchers.shouldBe
import java.io.File
import java.util.UUID
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
})

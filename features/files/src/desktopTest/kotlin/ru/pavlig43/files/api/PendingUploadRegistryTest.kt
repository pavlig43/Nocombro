package ru.pavlig43.files.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import java.nio.file.Files

/** Проверяет запись переданного времени в реестр незавершённых загрузок. */
class PendingUploadRegistryTest : FunSpec({
    test("pending upload timestamps use the supplied local clock") {
        val localNow = LocalDateTime(2026, 7, 14, 18, 30)
        val registry = PendingUploadRegistry(
            registryPath = Files.createTempDirectory("pending-upload-registry")
                .resolve("pending-uploads.tsv"),
            currentLocalDateTime = { localNow },
        )

        registry.markPending(
            objectKey = "files/experiment_entry/file-sync-id/report.pdf",
            localPath = "C:/managed/report.pdf",
        )

        registry.list().single().let { entry ->
            entry.firstSeenAt shouldBe localNow
            entry.lastAttemptAt shouldBe localNow
        }
    }
})

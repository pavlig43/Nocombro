package ru.pavlig43.database

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.SyncAnalysisReportWriter
import ru.pavlig43.database.data.sync.mirror.ExpenseMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlan
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPreview
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class SyncAnalysisReportTest : DesktopMainDispatcherFunSpec({

    test("report describes push and pull changes with escaped payload differences") {
        val oldAt = LocalDateTime(2026, 6, 12, 10, 0)
        val newAt = LocalDateTime(2026, 6, 12, 11, 0)
        val deletedAt = LocalDateTime(2026, 6, 12, 12, 0)
        val localChanged = ExpenseMirrorRow(
            syncId = "expense|1",
            transactionSyncId = null,
            expenseType = ExpenseType.OTHER,
            amount = 200,
            expenseDateTime = newAt,
            comment = "новая | строка\nвторая",
            updatedAt = newAt,
        )
        val remoteOld = localChanged.copy(
            transactionSyncId = "transaction-old",
            expenseType = ExpenseType.COMMISSION,
            amount = 100,
            expenseDateTime = oldAt,
            comment = "старая",
            updatedAt = oldAt,
        )
        val localDeleted = VendorMirrorRow(
            syncId = "deleted",
            displayName = "Удаляемый",
            comment = "",
            updatedAt = oldAt,
            deletedAt = deletedAt,
        )
        val remoteLive = localDeleted.copy(deletedAt = null)
        val remoteCreated = VendorMirrorRow(
            syncId = "remote-new",
            displayName = "Новый",
            comment = "текст",
            updatedAt = newAt,
        )
        val preview = MirrorReconciliationPreview(
            localSnapshot = snapshotLocal(localChanged, localDeleted),
            remoteSnapshot = snapshotRemote(remoteOld, remoteLive, remoteCreated),
            plan = MirrorReconciliationPlan(
                pushChanges = listOf(
                    MirrorPushEntityChange(MirrorSyncTable.EXPENSE, localChanged),
                    MirrorPushEntityChange(MirrorSyncTable.VENDOR, localDeleted),
                ),
                pullChanges = listOf(
                    MirrorPushEntityChange(MirrorSyncTable.VENDOR, remoteCreated),
                ),
            ),
        )
        val directory = Files.createTempDirectory("nocombro-report-test").toFile()
        val writer = SyncAnalysisReportWriter(
            reportDirectory = { directory },
        )

        val report = writer.write(preview).readText(StandardCharsets.UTF_8)

        report shouldContain "## Будет отправлено"
        report shouldContain "## Будет получено"
        report shouldContain "Snapshot Room: `12.06.2026 12:30`"
        report shouldContain "Snapshot YDB: `12.06.2026 12:31`"
        report shouldContain "| изменение |"
        report shouldContain "| удаление |"
        report shouldContain "| создание |"
        report shouldContain "expense\\|1"
        report shouldContain "новая \\| строка<br>вторая"
        report shouldContain "| transactionSyncId | transaction-old → null |"
        report shouldContain "| amount | 100 → 200 |"
        report shouldContain "| expenseType | COMMISSION → OTHER |"
        report shouldContain "| expenseDateTime | 12.06.2026 10:00 → 12.06.2026 11:00 |"
        report shouldNotContain "| updatedAt |"
        report shouldNotContain "| deletedAt |"
        report shouldNotContain "| syncId | expense"
    }

    test("writer atomically replaces latest report and keeps old files") {
        val directory = Files.createTempDirectory("nocombro-report-files").toFile()
        val oldReport = directory.resolve("sync-analysis-20260612-123456-789.md").apply {
            writeText("старый отчёт", StandardCharsets.UTF_8)
        }
        val writer = SyncAnalysisReportWriter(
            reportDirectory = { directory },
        )
        val preview = MirrorReconciliationPreview(
            localSnapshot = snapshotLocal(),
            remoteSnapshot = snapshotRemote(),
            plan = MirrorReconciliationPlan(emptyList(), emptyList()),
        )

        val first = writer.write(preview)
        val second = writer.write(preview)

        first shouldBe second
        first.name shouldBe "latest-sync-analysis.md"
        first.exists().shouldBeTrue()
        oldReport.exists().shouldBeTrue()
        directory.listFiles().orEmpty().none { it.extension == "tmp" }.shouldBeTrue()
        writer.latestReport().getOrThrow() shouldBe first
        writer.latestSnapshotAt() shouldBe LocalDateTime(2026, 6, 12, 12, 31)
        writer.latestHtmlSnapshotAt() shouldBe LocalDateTime(2026, 6, 12, 12, 31)
        writer.latestHtmlReport().getOrThrow().readText() shouldContain "<meta charset=\"utf-8\">"
        first.readText(StandardCharsets.UTF_8) shouldContain "Расхождений нет."
        first.readText(StandardCharsets.UTF_8) shouldContain
            "Снимок от 12.06.2026 12:31. Не обновляется при открытии."
        first.readBytes().toString(StandardCharsets.UTF_8) shouldContain "Отчёт синхронизации"
    }

    test("partial replacement keeps each files actual timestamp and cleans temporary files") {
        val directory = Files.createTempDirectory("sync-partial-report").toFile()
        try {
            val old = MirrorReconciliationPreview(snapshotLocal(), snapshotRemote(), MirrorReconciliationPlan(emptyList(), emptyList()))
            val writer = SyncAnalysisReportWriter(reportDirectory = { directory })
            writer.write(old)
            val fresh = old.copy(remoteSnapshot = old.remoteSnapshot.copy(loadedAt = LocalDateTime(2026, 6, 13, 9, 0)))
            val failingWriter = SyncAnalysisReportWriter(reportDirectory = { directory }, replaceReport = { source, target ->
                // Оба файла должны быть готовы ещё до первой замены.
                if (target.fileName.toString().endsWith(".md")) {
                    directory.listFiles().orEmpty().count { it.extension == "tmp" } shouldBe 2
                    directory.listFiles().orEmpty().filter { it.extension == "tmp" }.all { it.length() > 0 } shouldBe true
                    Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING)
                } else error("html replacement failed")
            })
            runCatching { failingWriter.write(fresh) }.exceptionOrNull()?.message shouldBe "html replacement failed"
            writer.latestSnapshotAt() shouldBe fresh.remoteSnapshot.loadedAt
            writer.latestHtmlSnapshotAt() shouldBe old.remoteSnapshot.loadedAt
            directory.listFiles().orEmpty().count { it.extension == "tmp" } shouldBe 0
        } finally {
            directory.listFiles().orEmpty().forEach { Files.delete(it.toPath()) }
            Files.delete(directory.toPath())
        }
    }

    test("Markdown alone never fabricates HTML and preparation failure preserves old reports") {
        val directory = Files.createTempDirectory("sync-old-markdown").toFile()
        try {
            directory.resolve("latest-sync-analysis.md").writeText("legacy markdown")
            val writer = SyncAnalysisReportWriter(reportDirectory = { directory })
            writer.latestReport().isSuccess shouldBe true
            writer.latestSnapshotAt() shouldBe null
            writer.latestHtmlReport().isFailure shouldBe true
            writer.latestHtmlSnapshotAt() shouldBe null
            val preview = MirrorReconciliationPreview(snapshotLocal(), snapshotRemote(), MirrorReconciliationPlan(emptyList(), emptyList()))
            val failing = SyncAnalysisReportWriter(reportDirectory = { directory }, replaceReport = { _, _ -> error("disk failure") })
            runCatching { failing.write(preview) }.isFailure shouldBe true
            directory.resolve("latest-sync-analysis.md").readText() shouldBe "legacy markdown"
            writer.latestHtmlReport().isFailure shouldBe true
            directory.listFiles().orEmpty().count { it.extension == "tmp" } shouldBe 0
        } finally {
            directory.listFiles().orEmpty().forEach { Files.delete(it.toPath()) }
            Files.delete(directory.toPath())
        }
    }
})

private fun snapshotLocal(vararg rows: ru.pavlig43.database.data.sync.mirror.MirrorSyncRow) =
    MirrorLocalSnapshot(
        loadedAt = LocalDateTime(2026, 6, 12, 12, 30),
        rowsByTable = rows.groupBy {
            when (it) {
                is ExpenseMirrorRow -> MirrorSyncTable.EXPENSE
                else -> MirrorSyncTable.VENDOR
            }
        },
    )

private fun snapshotRemote(vararg rows: ru.pavlig43.database.data.sync.mirror.MirrorSyncRow) =
    MirrorRemoteSnapshot(
        loadedAt = LocalDateTime(2026, 6, 12, 12, 31),
        rowsByTable = rows.groupBy {
            when (it) {
                is ExpenseMirrorRow -> MirrorSyncTable.EXPENSE
                else -> MirrorSyncTable.VENDOR
            }
        },
    )

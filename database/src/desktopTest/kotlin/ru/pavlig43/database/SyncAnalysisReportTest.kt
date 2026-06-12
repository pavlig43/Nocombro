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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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
            clock = fixedClock(),
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

    test("writer keeps existing reports and creates UTF-8 unique names") {
        val directory = Files.createTempDirectory("nocombro-report-files").toFile()
        val writer = SyncAnalysisReportWriter(
            reportDirectory = { directory },
            clock = fixedClock(),
        )
        val preview = MirrorReconciliationPreview(
            localSnapshot = snapshotLocal(),
            remoteSnapshot = snapshotRemote(),
            plan = MirrorReconciliationPlan(emptyList(), emptyList()),
        )

        val first = writer.write(preview)
        val second = writer.write(preview)

        first.name shouldBe "sync-analysis-20260612-123456-789.md"
        second.name shouldBe "sync-analysis-20260612-123456-790.md"
        first.exists().shouldBeTrue()
        first.readText(StandardCharsets.UTF_8) shouldContain "Расхождений нет."
        first.readBytes().toString(StandardCharsets.UTF_8) shouldContain "Отчёт синхронизации"
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

private fun fixedClock(): Clock =
    Clock.fixed(Instant.parse("2026-06-12T12:34:56.789Z"), ZoneOffset.UTC)

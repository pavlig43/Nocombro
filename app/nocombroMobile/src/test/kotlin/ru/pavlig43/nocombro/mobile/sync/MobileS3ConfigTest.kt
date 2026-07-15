package ru.pavlig43.nocombro.mobile.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class MobileS3ConfigTest {
    private val planner = MobileReconciliationPlanner()
    private val config = MobileS3Config(
        endpoint = "https://storage.yandexcloud.net",
        bucket = "nocombro",
        region = "ru-central1",
        accessKeyId = "access",
        secretAccessKey = "secret",
        keyPrefix = "mobile",
    )

    @Test
    fun remoteKeyAddsPrefixOnlyOnce() {
        val logicalKey = "files/experiment_entry/file-sync/report.pdf"
        val remoteKey = "mobile/files/experiment_entry/file-sync/report.pdf"

        assertEquals(remoteKey, config.remoteKey(logicalKey))
        assertEquals(remoteKey, config.remoteKey(remoteKey))
    }

    @Test
    fun normalizeObjectKeyRemovesPrefixBeforeNextPush() {
        val remoteKeyFromYdb = "mobile/files/experiment_entry/file-sync/report.pdf"
        val logicalKeyAfterPull = config.normalizeObjectKey(remoteKeyFromYdb)
        val ydbKeyForNextPush = config.normalizeObjectKey(logicalKeyAfterPull)

        assertEquals("files/experiment_entry/file-sync/report.pdf", logicalKeyAfterPull)
        assertEquals("files/experiment_entry/file-sync/report.pdf", ydbKeyForNextPush)
        assertEquals(remoteKeyFromYdb, config.remoteKey(ydbKeyForNextPush))
    }

    /** Проверяет перевод обратных косых черт в переносимый S3-ключ. */
    @Test
    fun ordinaryBackslashesAreNormalized() {
        assertEquals(
            "files/experiment_entry/report.pdf",
            config.normalizeObjectKey("files\\experiment_entry\\report.pdf"),
        )
    }

    /** Проверяет отказ для пустых, абсолютных и обходящих каталог ключей. */
    @Test
    fun unsafeLogicalKeysAreRejected() {
        listOf(
            "",
            "../secret",
            "..\\secret",
            "/absolute",
            "C:/absolute",
            "C:relative.txt",
            "mobile",
            "files/./secret",
        )
            .forEach { key ->
                assertFailsWith<IllegalArgumentException> { config.normalizeObjectKey(key) }
            }
    }

    /** Проверяет, что неверный ключ отклоняется до создания локального каталога. */
    @Test
    fun invalidDownloadKeyCreatesNoLocalDirectory() = runTest {
        val root = Files.createTempDirectory("invalid-mobile-s3-key")
        val target = root.resolve("not-created/report.pdf")
        val gateway = AwsKotlinMobileS3Gateway(config)

        val result = gateway.downloadFile("mobile", target.toString())

        assertTrue(result.isFailure)
        assertFalse(Files.exists(target.parent))
    }

    @Test
    fun pullSnapshotPushKeepsOnePrefix() {
        val logicalKey = "files/experiment_entry/file-sync/report.pdf"
        val remoteKeyFromYdb = "mobile/$logicalKey"
        val remoteAfterPull = snapshot(
            experiment(updatedAt = "2026-01-01T00:00:00"),
            entry(updatedAt = "2026-01-01T00:00:00"),
            file(
                remoteObjectKey = remoteKeyFromYdb,
                updatedAt = "2026-01-01T00:00:00",
            ),
        ).normalizeFileKeys(config)
        val pulledFile = remoteAfterPull.fileRows().single()
        val localSnapshotAfterPull = snapshot(
            experiment(updatedAt = "2026-01-02T00:00:00"),
            entry(updatedAt = "2026-01-02T00:00:00"),
            file(
                remoteObjectKey = pulledFile.remoteObjectKey.orEmpty(),
                updatedAt = "2026-01-02T00:00:00",
            ),
        )

        val pushRow = planner.plan(localSnapshotAfterPull, remoteAfterPull)
            .pushChanges
            .map(MobileMirrorChange::row)
            .filterIsInstance<MobileFileMirrorRow>()
            .single()

        assertEquals(logicalKey, pulledFile.remoteObjectKey)
        assertEquals(logicalKey, pushRow.remoteObjectKey)
        assertEquals(remoteKeyFromYdb, config.remoteKey(pushRow.remoteObjectKey.orEmpty()))
    }

    private fun snapshot(vararg rows: MobileMirrorRow): MobileMirrorSnapshot {
        return MobileMirrorSnapshot(
            loadedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            rowsByTable = rows.groupBy { row ->
                when (row) {
                    is MobileExperimentMirrorRow -> MobileMirrorTable.EXPERIMENT
                    is MobileExperimentEntryMirrorRow -> MobileMirrorTable.EXPERIMENT_ENTRY
                    is MobileExperimentReminderMirrorRow -> MobileMirrorTable.EXPERIMENT_REMINDER
                    is MobileFileMirrorRow -> MobileMirrorTable.FILE
                }
            },
        )
    }

    private fun MobileMirrorSnapshot.fileRows(): List<MobileFileMirrorRow> {
        return rowsByTable[MobileMirrorTable.FILE].orEmpty().filterIsInstance<MobileFileMirrorRow>()
    }

    private fun experiment(
        updatedAt: String,
    ) = MobileExperimentMirrorRow(
        syncId = "experiment-sync",
        title = "Experiment",
        ideaDescription = "Idea",
        isArchived = false,
        updatedAt = LocalDateTime.parse(updatedAt),
        deletedAt = null,
    )

    private fun entry(
        updatedAt: String,
    ) = MobileExperimentEntryMirrorRow(
        syncId = "entry-sync",
        experimentSyncId = "experiment-sync",
        entryDate = LocalDate.parse("2026-01-01"),
        createdAt = LocalDateTime.parse("2026-01-01T10:00:00"),
        content = "Entry content",
        updatedAt = LocalDateTime.parse(updatedAt),
        deletedAt = null,
    )

    private fun file(
        remoteObjectKey: String,
        updatedAt: String,
    ) = MobileFileMirrorRow(
        syncId = "file-sync",
        ownerType = MobileFileOwnerType.EXPERIMENT_ENTRY,
        ownerSyncId = "entry-sync",
        displayName = "report.pdf",
        path = "/tmp/report.pdf",
        remoteObjectKey = remoteObjectKey,
        remoteStorageProvider = "S3",
        updatedAt = LocalDateTime.parse(updatedAt),
        deletedAt = null,
    )
}

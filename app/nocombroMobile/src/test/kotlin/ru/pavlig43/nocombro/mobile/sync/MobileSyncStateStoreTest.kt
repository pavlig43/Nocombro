package ru.pavlig43.nocombro.mobile.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MobileSyncStateStoreTest {
    @Test
    fun `two launches inside one hour allow one background check`() = runTest {
        val dataStoreFile = temporaryDataStoreFile()
        val store = DataStoreMobileSyncStateStore(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { dataStoreFile },
            )
        )
        val firstAttempt = 10_000L

        assertTrue(store.tryRecordBackgroundAttempt(firstAttempt, ONE_HOUR_MILLIS))
        assertFalse(store.tryRecordBackgroundAttempt(firstAttempt + 1_000L, ONE_HOUR_MILLIS))
        assertTrue(store.tryRecordBackgroundAttempt(firstAttempt + ONE_HOUR_MILLIS, ONE_HOUR_MILLIS))
    }

    @Test
    fun `cached status is available without a remote request`() = runTest {
        val dataStoreFile = temporaryDataStoreFile()
        val store = DataStoreMobileSyncStateStore(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { dataStoreFile },
            )
        )
        val checkedAt = LocalDateTime(2026, 8, 2, 1, 2, 3)

        store.saveStatus(
            status = MobileSyncStatus(
                configured = true,
                checkedAt = checkedAt,
                localChanges = 2,
                remoteChanges = 3,
            ),
            error = null,
        )

        val cached = assertNotNull(store.loadCachedStatus())
        assertTrue(cached.configured)
        assertEquals(checkedAt, cached.checkedAt)
        assertEquals(2, cached.localChanges)
        assertEquals(3, cached.remoteChanges)
    }

    @Test
    fun `launch inside one hour can use cached status and report`() = runTest {
        val dataStoreFile = temporaryDataStoreFile()
        val stateStore = DataStoreMobileSyncStateStore(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { dataStoreFile },
            )
        )
        val filesDir = Files.createTempDirectory("nocombro-mobile-report-state-").toFile()
        val reportStore = MobileSyncReportStore(filesDir)
        val checkedAt = LocalDateTime(2026, 8, 2, 12, 0, 0)
        val firstAttempt = 10_000L
        stateStore.tryRecordBackgroundAttempt(firstAttempt, ONE_HOUR_MILLIS)
        stateStore.saveStatus(
            status = MobileSyncStatus(
                configured = true,
                checkedAt = checkedAt,
                localChanges = 1,
                remoteChanges = 2,
            ),
            error = null,
        )
        reportStore.write(
            MobileSyncPreview(
                snapshotAt = checkedAt,
                localChanges = emptyList(),
                remoteChanges = emptyList(),
            )
        ).getOrThrow()

        val shouldCheckRemote = stateStore.tryRecordBackgroundAttempt(
            firstAttempt + 1_000L,
            ONE_HOUR_MILLIS,
        )

        assertFalse(shouldCheckRemote)
        assertEquals(checkedAt, assertNotNull(stateStore.loadCachedStatus()).checkedAt)
        assertEquals(checkedAt, reportStore.latestSnapshotAt())
    }
}

private fun temporaryDataStoreFile(): File {
    val file = File.createTempFile("nocombro-mobile-sync-", ".preferences_pb")
    check(file.delete())
    file.deleteOnExit()
    return file
}

private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

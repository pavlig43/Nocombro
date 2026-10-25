package ru.pavlig43.nocombro.mobile.sync

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class MobileSyncStateStoreTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Test
    fun `two launches inside one hour allow one background check`() = runTest {
        withStateStore { store ->
            val firstAttempt = 10_000L

            assertTrue(store.tryRecordBackgroundAttempt(firstAttempt, ONE_HOUR_MILLIS))
            assertFalse(store.tryRecordBackgroundAttempt(firstAttempt + 1_000L, ONE_HOUR_MILLIS))
            assertTrue(store.tryRecordBackgroundAttempt(firstAttempt + ONE_HOUR_MILLIS, ONE_HOUR_MILLIS))
        }
    }

    @Test
    fun `cached status is available without a remote request`() = runTest {
        withStateStore { store ->
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
    }

    @Test
    fun `launch inside one hour can use cached status and report`() = runTest {
        withStateStore { stateStore ->
            val reportStore = MobileSyncReportStore(temporaryFolder.newFolder("reports"))
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

    @Test
    fun `reopened datastore keeps cached status and background check limit`() = runTest {
        val dataStoreFile = File(temporaryFolder.root, "sync.preferences_pb")
        val checkedAt = LocalDateTime(2026, 8, 2, 12, 0, 0)
        val firstAttempt = 10_000L
        val expectedStatus = MobileCachedSyncStatus(
            configured = true,
            localChanges = 2,
            remoteChanges = 3,
            checkedAt = checkedAt,
            error = "Saved sync error",
            hasConflicts = false,
        )

        withStateStore(dataStoreFile) { store ->
            assertTrue(store.tryRecordBackgroundAttempt(firstAttempt, ONE_HOUR_MILLIS))
            store.saveStatus(
                status = MobileSyncStatus(
                    configured = true,
                    checkedAt = checkedAt,
                    localChanges = 2,
                    remoteChanges = 3,
                ),
                error = expectedStatus.error,
            )
        }

        assertTrue(dataStoreFile.isFile)
        assertTrue(dataStoreFile.length() > 0)

        withStateStore(dataStoreFile) { store ->
            assertEquals(expectedStatus, store.loadCachedStatus())
            assertFalse(
                store.tryRecordBackgroundAttempt(firstAttempt + ONE_HOUR_MILLIS - 1, ONE_HOUR_MILLIS)
            )
            assertTrue(
                store.tryRecordBackgroundAttempt(firstAttempt + ONE_HOUR_MILLIS, ONE_HOUR_MILLIS)
            )
        }
    }

    private suspend fun TestScope.withStateStore(
        dataStoreFile: File = File(temporaryFolder.root, "sync.preferences_pb"),
        block: suspend (MobileSyncStateStore) -> Unit,
    ) {
        val dataStoreJob = Job(backgroundScope.coroutineContext[Job])
        try {
            val dataStore = DataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = TestPreferencesSerializer,
                    producePath = { dataStoreFile.toOkioPath() },
                ),
                scope = CoroutineScope(backgroundScope.coroutineContext + dataStoreJob),
            )
            block(DataStoreMobileSyncStateStore(dataStore))
        } finally {
            withContext(NonCancellable) {
                dataStoreJob.cancelAndJoin()
            }
        }
    }
}

private object TestPreferencesSerializer : OkioSerializer<Preferences> {
    override val defaultValue: Preferences
        get() = PreferencesFileSerializer.defaultValue

    override suspend fun readFrom(source: BufferedSource): Preferences =
        PreferencesFileSerializer.readFrom(source.inputStream())

    override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
        PreferencesFileSerializer.writeTo(t, sink.outputStream())
    }
}

private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

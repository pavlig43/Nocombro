package ru.pavlig43.rootnocombro.api.component

import io.kotest.matchers.shouldBe
import org.koin.dsl.koinApplication
import ru.pavlig43.datastore.DATASTORE_PATH_PROPERTY
import ru.pavlig43.datastore.SyncCheckAttemptStore
import ru.pavlig43.datastore.getSettingsRepository
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import java.nio.file.Files

/** Проверяет часовой лимит фоновой проверки на постоянном DataStore. */
class SyncCheckAttemptStoreTest : DesktopMainDispatcherFunSpec({
    test("two launches within an hour allow only one background check") {
        val dataStorePath = Files.createTempDirectory("sync-check-attempt")
            .resolve("settings.preferences_pb")
        System.setProperty(DATASTORE_PATH_PROPERTY, dataStorePath.toString())
        val application = koinApplication {
            modules(getSettingsRepository())
        }
        try {
            val store = application.koin.get<SyncCheckAttemptStore>()
            val firstAttemptAt = 10_000_000L

            store.tryRecordBackgroundAttempt(firstAttemptAt, ONE_HOUR_MILLIS) shouldBe true
            store.tryRecordBackgroundAttempt(
                firstAttemptAt + ONE_HOUR_MILLIS - 1,
                ONE_HOUR_MILLIS,
            ) shouldBe false
            store.tryRecordBackgroundAttempt(
                firstAttemptAt + ONE_HOUR_MILLIS,
                ONE_HOUR_MILLIS,
            ) shouldBe true
        } finally {
            application.close()
            System.clearProperty(DATASTORE_PATH_PROPERTY)
        }
    }
})

private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

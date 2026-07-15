package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

/** Проверяет монотонность UTC-версий настольной синхронизации. */
class SyncMetadataTest : DesktopMainDispatcherFunSpec({
    test("sync version advances by one nanosecond when the device clock is behind") {
        val futureVersion = LocalDateTime.parse("2099-01-01T00:00:00.123456789")

        defaultUpdatedAt(futureVersion) shouldBe
            LocalDateTime.parse("2099-01-01T00:00:00.123456790")
    }
})

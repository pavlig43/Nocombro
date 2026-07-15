package ru.pavlig43.nocombro.mobile.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime

/** Проверяет монотонность UTC-версий мобильной синхронизации. */
class MobileSyncTimeTest {
    /** Проверяет шаг в одну наносекунду при отставших часах устройства. */
    @Test
    fun versionAdvancesByOneNanosecondWhenDeviceClockIsBehind() {
        val futureVersion = LocalDateTime.parse("2099-01-01T00:00:00.123456789")

        assertEquals(
            LocalDateTime.parse("2099-01-01T00:00:00.123456790"),
            mobileUpdatedAt(futureVersion),
        )
    }
}

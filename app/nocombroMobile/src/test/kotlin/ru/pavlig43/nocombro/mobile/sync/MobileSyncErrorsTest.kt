package ru.pavlig43.nocombro.mobile.sync

import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MobileSyncErrorsTest {
    @Test
    fun `resource error does not expose stream query in UI`() {
        val throwable = SQLException(
            "Cannot execute 'STREAM_QUERY SELECT secret' with " +
                "CLIENT_RESOURCE_EXHAUSTED(code=401020)"
        )

        val message = throwable.mobileSyncDisplayMessage("Не удалось получить снимок YDB")

        assertEquals(
            "Не удалось получить снимок YDB: YDB временно перегружена. Повторите позже.",
            message,
        )
        assertFalse(message.contains("STREAM_QUERY"))
        assertFalse(message.contains("SELECT secret"))
    }

    @Test
    fun `other technical error is replaced by stage text`() {
        val message = SQLException("BAD_REQUEST SELECT secret")
            .mobileSyncDisplayMessage("Не удалось получить снимок YDB")

        assertEquals("Не удалось получить снимок YDB", message)
    }
}

package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import ru.pavlig43.database.data.sync.mirror.isYdbResourceExhausted
import ru.pavlig43.database.data.sync.mirror.retryYdbResourceExhausted
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import java.sql.SQLException

/** Проверяет повторы чтения после временного отказа YDB по ресурсам. */
class YdbResourceExhaustedRetryTest : DesktopMainDispatcherFunSpec({
    test("resource exhausted read is retried without a real delay") {
        var attempts = 0
        val delays = mutableListOf<Long>()

        val result = retryYdbResourceExhausted(
            retryDelaysMillis = listOf(2_000L, 5_000L),
            delayAction = { delays += it },
        ) {
            attempts += 1
            if (attempts < 3) {
                throw SQLException("CLIENT_RESOURCE_EXHAUSTED(code=401020)")
            }
            "ok"
        }

        result shouldBe "ok"
        attempts shouldBe 3
        delays shouldBe listOf(2_000L, 5_000L)
    }

    test("non retryable JDBC error is returned at once") {
        val error = SQLException("BAD_REQUEST")
        var attempts = 0

        val thrown = runCatching {
            retryYdbResourceExhausted(
                retryDelaysMillis = listOf(0L, 0L),
                delayAction = {},
            ) {
                attempts += 1
                throw error
            }
        }.exceptionOrNull()

        thrown shouldBe error
        attempts shouldBe 1
        error.isYdbResourceExhausted() shouldBe false
    }
})

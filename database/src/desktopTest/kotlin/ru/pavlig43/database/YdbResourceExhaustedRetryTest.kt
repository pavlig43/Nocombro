package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import ru.pavlig43.database.data.sync.mirror.isYdbResourceExhausted
import ru.pavlig43.database.data.sync.mirror.retryYdbResourceExhausted
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.Properties

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

    test("resource exhausted read closes connection before retry") {
        val first = fakeConnection {
            throw SQLException("CLIENT_RESOURCE_EXHAUSTED(code=401020)")
        }
        val second = fakeConnection(::emptyResultSet)
        val connections = ArrayDeque(listOf(first.connection, second.connection))
        val connectionProperties = mutableListOf<Properties>()
        val gateway = YdbJdbcMirrorSyncGateway(
            config = YdbMirrorJdbcConfig(
                jdbcUrl = "jdbc:ydb:test",
                authToken = "token",
                serviceAccountFile = null,
                tableRoot = "",
            ),
            connectionFactory = { _, properties ->
                connectionProperties += Properties().apply { putAll(properties) }
                connections.removeFirst()
            },
        )

        val result = gateway.loadRemoteSnapshot(listOf(MirrorSyncTable.VENDOR))

        result.isSuccess shouldBe true
        first.isClosed() shouldBe true
        second.isClosed() shouldBe false
        connectionProperties.size shouldBe 2
        connectionProperties.all { it.getProperty("useStreamResultSets") == "false" } shouldBe true
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

private data class FakeConnection(
    val connection: Connection,
    val isClosed: () -> Boolean,
)

private fun fakeConnection(executeQuery: () -> ResultSet): FakeConnection {
    var closed = false
    val statement = Proxy.newProxyInstance(
        Statement::class.java.classLoader,
        arrayOf(Statement::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "executeQuery" -> executeQuery()
            "close" -> null
            "isClosed" -> false
            else -> defaultValue(method.returnType)
        }
    } as Statement
    val connection = Proxy.newProxyInstance(
        Connection::class.java.classLoader,
        arrayOf(Connection::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "createStatement" -> statement
            "close" -> {
                closed = true
                null
            }
            "isClosed" -> closed
            "equals" -> proxy === arguments?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            else -> defaultValue(method.returnType)
        }
    } as Connection
    return FakeConnection(connection = connection, isClosed = { closed })
}

private fun emptyResultSet(): ResultSet {
    return Proxy.newProxyInstance(
        ResultSet::class.java.classLoader,
        arrayOf(ResultSet::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "next" -> false
            "close" -> null
            "isClosed" -> false
            else -> defaultValue(method.returnType)
        }
    } as ResultSet
}

private fun defaultValue(type: Class<*>): Any? = when (type) {
    java.lang.Boolean.TYPE -> false
    java.lang.Byte.TYPE -> 0.toByte()
    java.lang.Short.TYPE -> 0.toShort()
    java.lang.Integer.TYPE -> 0
    java.lang.Long.TYPE -> 0L
    java.lang.Float.TYPE -> 0f
    java.lang.Double.TYPE -> 0.0
    java.lang.Character.TYPE -> '\u0000'
    else -> null
}

package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import ru.pavlig43.database.data.sync.mirror.isYdbResourceExhausted
import ru.pavlig43.database.data.sync.mirror.retryYdbResourceExhausted
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.Properties

/** Проверяет безопасные повторы чтения и условной batch-записи после отказа YDB по ресурсам. */
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

    test("conditional batch write reconnects and retries as one request") {
        val rows = listOf(
            vendorRow("vendor-1"),
            vendorRow("vendor-2"),
            vendorRow("vendor-3"),
        )
        var failedBatchSize = 0
        var successfulBatchSize = 0
        val first = fakeBatchPushConnection { batchSize ->
            failedBatchSize = batchSize
            throw SQLException("CLIENT_RESOURCE_EXHAUSTED(code=401020)")
        }
        val second = fakeBatchPushConnection { batchSize ->
            successfulBatchSize = batchSize
            vendorResultSet(rows)
        }
        val connections = ArrayDeque(listOf(first.connection, second.connection))
        val gateway = YdbJdbcMirrorSyncGateway(
            config = YdbMirrorJdbcConfig(
                jdbcUrl = "jdbc:ydb:test",
                authToken = "token",
                serviceAccountFile = null,
                tableRoot = "",
            ),
            connectionFactory = { _, _ -> connections.removeFirst() },
        )
        val changes = rows.map { row ->
            MirrorPushEntityChange(MirrorSyncTable.VENDOR, row)
        }

        val result = gateway.pushMirrorState(changes).getOrThrow()

        result.acceptedChanges shouldBe changes
        result.rejectedChanges shouldBe emptyList()
        failedBatchSize shouldBe rows.size
        successfulBatchSize shouldBe rows.size
        first.isClosed() shouldBe true
        second.isClosed() shouldBe false
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

private fun fakeBatchPushConnection(executeBatch: (Int) -> ResultSet): FakeConnection {
    var closed = false
    var batchSize = 0
    var pendingResultSet: ResultSet? = null
    var resultAdvanced = false
    val statement = Proxy.newProxyInstance(
        PreparedStatement::class.java.classLoader,
        arrayOf(PreparedStatement::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "addBatch" -> {
                batchSize += 1
                null
            }
            "executeBatch" -> {
                pendingResultSet = executeBatch(batchSize)
                resultAdvanced = false
                IntArray(batchSize)
            }
            "getResultSet" -> pendingResultSet.takeIf { resultAdvanced }
            "getUpdateCount" -> if (resultAdvanced) -1 else 1
            "getMoreResults" -> {
                resultAdvanced = true
                pendingResultSet != null
            }
            "close" -> null
            "isClosed" -> false
            else -> defaultValue(method.returnType)
        }
    } as PreparedStatement
    val connection = Proxy.newProxyInstance(
        Connection::class.java.classLoader,
        arrayOf(Connection::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "prepareStatement" -> statement
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

private fun vendorRow(syncId: String) = VendorMirrorRow(
    syncId = syncId,
    displayName = "Vendor $syncId",
    comment = "",
    updatedAt = LocalDateTime.parse("2026-08-05T20:00:00"),
    deletedAt = null,
)

private fun vendorResultSet(rows: List<VendorMirrorRow>): ResultSet {
    var index = -1
    return Proxy.newProxyInstance(
        ResultSet::class.java.classLoader,
        arrayOf(ResultSet::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "next" -> {
                index += 1
                index < rows.size
            }
            "getString" -> when (arguments?.firstOrNull()) {
                "sync_id" -> rows[index].syncId
                "display_name" -> rows[index].displayName
                "comment" -> rows[index].comment
                "updated_at" -> rows[index].updatedAt.toString()
                "deleted_at" -> null
                else -> null
            }
            "close" -> null
            "isClosed" -> false
            else -> defaultValue(method.returnType)
        }
    } as ResultSet
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

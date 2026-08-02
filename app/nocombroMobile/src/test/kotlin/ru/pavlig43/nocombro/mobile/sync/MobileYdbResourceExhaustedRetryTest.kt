package ru.pavlig43.nocombro.mobile.sync

import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MobileYdbResourceExhaustedRetryTest {
    @Test
    fun `resource exhausted read retries at most three times`() {
        var attempts = 0
        val delays = mutableListOf<Long>()

        val result = retryMobileYdbResourceExhausted(
            retryDelaysMillis = listOf(2_000L, 5_000L),
            delayAction = { delays += it },
        ) {
            attempts += 1
            if (attempts < 3) {
                throw SQLException("CLIENT_RESOURCE_EXHAUSTED(code=401020)")
            }
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(3, attempts)
        assertEquals(listOf(2_000L, 5_000L), delays)
    }

    @Test
    fun `snapshot closes failed connection and disables streaming result sets`() {
        var openedConnections = 0
        var closedConnections = 0
        var executedQueries = 0
        val connectionProperties = mutableListOf<Properties>()
        val gateway = MobileYdbMirrorGateway(
            config = MobileYdbConfig(
                jdbcUrl = "jdbc:ydb:test",
                token = "token",
            ),
            serviceAccountJson = null,
            connectionFactory = { _, properties ->
                val connectionIndex = openedConnections++
                connectionProperties += Properties().apply { putAll(properties) }
                fakeConnection(
                    executeQuery = {
                        executedQueries += 1
                        if (connectionIndex == 0) {
                            throw SQLException("CLIENT_RESOURCE_EXHAUSTED(code=401020)")
                        }
                        emptyResultSet()
                    },
                    onClose = { closedConnections += 1 },
                )
            },
            delayAction = {},
        )

        val result = gateway.loadSnapshot()

        assertTrue(result.isSuccess)
        assertEquals(5, openedConnections)
        assertEquals(5, closedConnections)
        assertEquals(5, executedQueries)
        assertTrue(connectionProperties.all {
            it.getProperty("useStreamResultSets") == "false"
        })
    }

    @Test
    fun `successful snapshot reads each mobile table once`() {
        var executedQueries = 0
        val gateway = MobileYdbMirrorGateway(
            config = MobileYdbConfig(
                jdbcUrl = "jdbc:ydb:test",
                token = "token",
            ),
            serviceAccountJson = null,
            connectionFactory = { _, _ ->
                fakeConnection(
                    executeQuery = {
                        executedQueries += 1
                        emptyResultSet()
                    },
                    onClose = {},
                )
            },
            delayAction = {},
        )

        val result = gateway.loadSnapshot()

        assertTrue(result.isSuccess)
        assertEquals(MobileMirrorTable.entries.size, executedQueries)
    }

    @Test
    fun `non resource error is not retried`() {
        var attempts = 0
        val error = SQLException("BAD_REQUEST")

        val thrown = runCatching {
            retryMobileYdbResourceExhausted(
                retryDelaysMillis = listOf(0L, 0L),
                delayAction = {},
            ) {
                attempts += 1
                throw error
            }
        }.exceptionOrNull()

        assertTrue(thrown === error)
        assertEquals(1, attempts)
        assertFalse(error.isMobileYdbResourceExhausted())
    }
}

private fun fakeConnection(
    executeQuery: () -> ResultSet,
    onClose: () -> Unit,
): Connection {
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
    return Proxy.newProxyInstance(
        Connection::class.java.classLoader,
        arrayOf(Connection::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "createStatement" -> statement
            "close" -> {
                onClose()
                null
            }
            "isClosed" -> false
            "equals" -> proxy === arguments?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            else -> defaultValue(method.returnType)
        }
    } as Connection
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

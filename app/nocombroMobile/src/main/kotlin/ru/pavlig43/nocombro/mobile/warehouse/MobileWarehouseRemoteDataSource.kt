package ru.pavlig43.nocombro.mobile.warehouse

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Properties
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.sync.MobileIamTokenSupplier
import ru.pavlig43.nocombro.mobile.sync.MobileYdbConfig
import ru.pavlig43.nocombro.mobile.sync.retryMobileYdbResourceExhausted

interface MobileWarehouseRemoteDataSource {
    fun loadSnapshot(): Result<MobileWarehouseSnapshot>
}

class YdbMobileWarehouseRemoteDataSource(
    private val config: MobileYdbConfig,
    private val serviceAccountJson: String?,
    private val connectionFactory: (String, Properties) -> Connection = { jdbcUrl, properties ->
        DriverManager.getConnection(jdbcUrl, properties)
    },
    private val delayAction: (Long) -> Unit = Thread::sleep,
) : MobileWarehouseRemoteDataSource {
    override fun loadSnapshot(): Result<MobileWarehouseSnapshot> = runCatching {
        val snapshotAt = getCurrentLocalDateTime()
        val products = loadRows(PRODUCT_QUERY) { resultSet ->
            RemoteWarehouseProductRow(
                syncId = resultSet.getString("sync_id"),
                type = resultSet.getString("type"),
                displayName = resultSet.getString("display_name"),
                deletedAt = resultSet.nullableDateTime("deleted_at"),
            )
        }
        val batches = loadRows(BATCH_QUERY) { resultSet ->
            RemoteWarehouseBatchRow(
                syncId = resultSet.getString("sync_id"),
                productSyncId = resultSet.getString("product_sync_id"),
                deletedAt = resultSet.nullableDateTime("deleted_at"),
            )
        }
        val movements = loadRows(BATCH_MOVEMENT_QUERY) { resultSet ->
            RemoteWarehouseMovementRow(
                syncId = resultSet.getString("sync_id"),
                batchSyncId = resultSet.getString("batch_sync_id"),
                movementType = resultSet.getString("movement_type"),
                count = resultSet.getLong("count"),
                transactionSyncId = resultSet.getString("transaction_sync_id"),
                storageLocation = resultSet.getString("storage_location"),
                deletedAt = resultSet.nullableDateTime("deleted_at"),
            )
        }
        val transactions = loadRows(TRANSACTION_QUERY) { resultSet ->
            RemoteWarehouseTransactionRow(
                syncId = resultSet.getString("sync_id"),
                transactionType = resultSet.getString("transaction_type"),
                createdAt = LocalDateTime.parse(resultSet.getString("created_at")),
                deletedAt = resultSet.nullableDateTime("deleted_at"),
            )
        }
        buildMobileWarehouseSnapshot(snapshotAt, products, batches, movements, transactions)
    }

    private fun <T> loadRows(
        query: String,
        mapper: (ResultSet) -> T,
    ): List<T> = retryMobileYdbResourceExhausted(delayAction = delayAction) {
        withConnection { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(query.withTableRoot()).use { resultSet ->
                    buildList {
                        while (resultSet.next()) add(mapper(resultSet))
                    }
                }
            }
        }
    }

    private fun String.withTableRoot(): String {
        val root = config.tableRoot.trim().trim('/')
        if (root.isEmpty()) return this
        return replace("`product`", "`$root/product`")
            .replace("`batch`", "`$root/batch`")
            .replace("`batch_movement`", "`$root/batch_movement`")
            .replace("`transact`", "`$root/transact`")
    }

    private fun <T> withConnection(block: (Connection) -> T): T {
        val properties = Properties().apply {
            setProperty("useStreamResultSets", "false")
            serviceAccountJson?.takeIf(String::isNotBlank)?.let {
                put("tokenProvider", MobileIamTokenSupplier(it))
            } ?: config.token?.takeIf(String::isNotBlank)?.let { setProperty("token", it) }
        }
        return connectionFactory(config.jdbcUrl, properties).use(block)
    }
}

private fun ResultSet.nullableDateTime(column: String): LocalDateTime? =
    getString(column)?.let(LocalDateTime::parse)

private const val PRODUCT_QUERY =
    "SELECT sync_id, type, display_name, deleted_at FROM `product`"
private const val BATCH_QUERY =
    "SELECT sync_id, product_sync_id, deleted_at FROM `batch`"
private const val BATCH_MOVEMENT_QUERY =
    "SELECT sync_id, batch_sync_id, movement_type, count, transaction_sync_id, " +
        "storage_location, deleted_at FROM `batch_movement`"
private const val TRANSACTION_QUERY =
    "SELECT sync_id, transaction_type, created_at, deleted_at FROM `transact`"

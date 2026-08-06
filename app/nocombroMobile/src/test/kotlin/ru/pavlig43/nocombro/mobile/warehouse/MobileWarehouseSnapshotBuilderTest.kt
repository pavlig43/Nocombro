package ru.pavlig43.nocombro.mobile.warehouse

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MobileWarehouseSnapshotBuilderTest {
    @Test
    fun `builds both balances includes zero and negative and ignores future and tombstones`() {
        val snapshot = buildMobileWarehouseSnapshot(
            snapshotAt = dateTime("2026-08-06T14:32:00"),
            products = listOf(
                product("sugar", "Сахар"),
                product("vanilla", "Ваниль"),
                product("unused", "Без движений"),
                product("deleted", "Удалён", deletedAt = "2026-08-05T00:00:00"),
            ),
            batches = listOf(
                batch("sugar-batch", "sugar"),
                batch("vanilla-batch", "vanilla"),
            ),
            transactions = listOf(
                transaction("past", "2026-08-06T13:00:00"),
                transaction("future", "2026-08-06T15:00:00"),
            ),
            movements = listOf(
                movement("main-in", "sugar-batch", "INCOMING", 2_000, "past", null),
                movement("main-out", "sugar-batch", "OUTGOING", 500, "past", "MAIN"),
                movement("experimental", "sugar-batch", "INCOMING", 750, "past", "EXPERIMENTAL"),
                movement("negative", "vanilla-batch", "OUTGOING", 120, "past", "MAIN"),
                movement("future", "sugar-batch", "INCOMING", 9_000, "future", "MAIN"),
                movement(
                    "deleted",
                    "sugar-batch",
                    "INCOMING",
                    8_000,
                    "past",
                    "MAIN",
                    deletedAt = "2026-08-06T14:00:00",
                ),
            ),
        )

        assertEquals("2026-08-06T14:32", snapshot.updatedAt.toString())
        assertEquals(
            MobileWarehouseProduct("sugar", "Сахар", 1_500, 750),
            snapshot.products.single { it.productSyncId == "sugar" },
        )
        assertEquals(-120, snapshot.products.single { it.productSyncId == "vanilla" }.mainBalance)
        assertEquals(
            MobileWarehouseProduct("unused", "Без движений", 0, 0),
            snapshot.products.single { it.productSyncId == "unused" },
        )
        assertNull(snapshot.products.singleOrNull { it.productSyncId == "deleted" })
    }

    @Test
    fun `unknown enum values fail the complete snapshot`() {
        assertFailsWith<IllegalArgumentException> {
            buildMobileWarehouseSnapshot(
                snapshotAt = dateTime("2026-08-06T14:32:00"),
                products = listOf(product("p", "Товар", type = "UNKNOWN")),
                batches = emptyList(),
                movements = emptyList(),
                transactions = emptyList(),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            buildMobileWarehouseSnapshot(
                snapshotAt = dateTime("2026-08-06T14:32:00"),
                products = listOf(product("p", "Товар")),
                batches = listOf(batch("b", "p")),
                movements = listOf(movement("m", "b", "SIDEWAYS", 1, "t", "MAIN")),
                transactions = listOf(transaction("t", "2026-08-06T13:00:00")),
            )
        }

        assertFailsWith<IllegalStateException> {
            buildMobileWarehouseSnapshot(
                snapshotAt = dateTime("2026-08-06T14:32:00"),
                products = listOf(product("p", "Товар")),
                batches = listOf(batch("b", "p")),
                movements = listOf(movement("m", "b", "INCOMING", 1, "t", "UNKNOWN")),
                transactions = listOf(transaction("t", "2026-08-06T13:00:00")),
            )
        }
    }

    @Test
    fun `broken active relationships fail instead of producing a partial snapshot`() {
        assertFailsWith<IllegalArgumentException> {
            buildMobileWarehouseSnapshot(
                snapshotAt = dateTime("2026-08-06T14:32:00"),
                products = listOf(product("p", "Товар")),
                batches = listOf(batch("broken-batch", "missing-product")),
                movements = emptyList(),
                transactions = emptyList(),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            buildMobileWarehouseSnapshot(
                snapshotAt = dateTime("2026-08-06T14:32:00"),
                products = listOf(product("p", "Товар")),
                batches = listOf(batch("b", "p")),
                movements = listOf(movement("m", "missing-batch", "INCOMING", 1, "t", "MAIN")),
                transactions = listOf(transaction("t", "2026-08-06T13:00:00")),
            )
        }
    }
}

private fun product(
    syncId: String,
    displayName: String,
    type: String = "FOOD_BASE",
    deletedAt: String? = null,
) = RemoteWarehouseProductRow(syncId, type, displayName, deletedAt?.let(::dateTime))

private fun batch(
    syncId: String,
    productSyncId: String,
) = RemoteWarehouseBatchRow(syncId, productSyncId)

private fun transaction(
    syncId: String,
    createdAt: String,
) = RemoteWarehouseTransactionRow(syncId, "BUY", dateTime(createdAt))

private fun movement(
    syncId: String,
    batchSyncId: String,
    type: String,
    count: Long,
    transactionSyncId: String,
    storageLocation: String?,
    deletedAt: String? = null,
) = RemoteWarehouseMovementRow(
    syncId = syncId,
    batchSyncId = batchSyncId,
    movementType = type,
    count = count,
    transactionSyncId = transactionSyncId,
    storageLocation = storageLocation,
    deletedAt = deletedAt?.let(::dateTime),
)

private fun dateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

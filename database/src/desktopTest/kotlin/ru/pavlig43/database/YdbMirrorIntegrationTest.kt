package ru.pavlig43.database

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.BatchCostPriceMirrorRow
import ru.pavlig43.database.data.sync.mirror.BatchMirrorRow
import ru.pavlig43.database.data.sync.mirror.DeclarationMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlanner
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRow
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.ProductMirrorRow
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow
import ru.pavlig43.database.data.sync.mirror.YdbJdbcMirrorSyncGateway
import ru.pavlig43.database.data.sync.mirror.YdbMirrorJdbcConfig
import ru.pavlig43.database.data.sync.mirror.markDeleted
import ru.pavlig43.testkit.database.createManagedTestDatabase
import java.util.UUID

class YdbMirrorIntegrationTest : FunSpec({

    val smokeEnabled = System.getenv("NOCOMBRO_YDB_SMOKE")
        ?.equals("true", ignoreCase = true)
        ?: false

    test("real YDB mirror supports table availability push pull newest wins and tombstones")
        .config(enabled = smokeEnabled) {
            val config = YdbMirrorJdbcConfig.fromEnvironment()
            val gateway = YdbJdbcMirrorSyncGateway(config)
            val syncId = "codex-smoke-${UUID.randomUUID()}"
            val olderAt = LocalDateTime(2026, 6, 11, 10, 0)
            val newerAt = LocalDateTime(2026, 6, 11, 10, 1)
            val deletedAt = LocalDateTime(2026, 6, 11, 10, 2)

            val status = gateway.getStatus()
            shouldNotThrowAny {
                check(status.error == null) { status.error.orEmpty() }
            }
            status.availableTables shouldContainAll
                MirrorSyncTable.mirroredBusinessTables.map(MirrorSyncTable::tableName)

            val older = VendorMirrorRow(
                syncId = syncId,
                displayName = "Codex smoke older",
                comment = "",
                updatedAt = olderAt,
            )
            gateway.pushMirrorState(
                listOf(MirrorPushEntityChange(MirrorSyncTable.VENDOR, older))
            ).getOrThrow()

            val newer = older.copy(
                displayName = "Codex smoke newer",
                updatedAt = newerAt,
            )
            gateway.pushMirrorState(
                listOf(MirrorPushEntityChange(MirrorSyncTable.VENDOR, newer))
            ).getOrThrow()

            val pulledNewer = gateway.loadRemoteSnapshot(listOf(MirrorSyncTable.VENDOR))
                .getOrThrow()
                .rowsByTable
                .getValue(MirrorSyncTable.VENDOR)
                .filterIsInstance<VendorMirrorRow>()
                .single { it.syncId == syncId }
            pulledNewer.displayName shouldBe newer.displayName
            pulledNewer.updatedAt shouldBe newerAt

            val tombstone = newer.copy(
                updatedAt = deletedAt,
                deletedAt = deletedAt,
            )
            gateway.pushMirrorState(
                listOf(MirrorPushEntityChange(MirrorSyncTable.VENDOR, tombstone))
            ).getOrThrow()

            val pulledTombstone = gateway.loadRemoteSnapshot(listOf(MirrorSyncTable.VENDOR))
                .getOrThrow()
                .rowsByTable
                .getValue(MirrorSyncTable.VENDOR)
                .filterIsInstance<VendorMirrorRow>()
                .single { it.syncId == syncId }
            pulledTombstone.deletedAt shouldBe deletedAt
            status.availableTables shouldContain MirrorSyncTable.BATCH_COST_PRICE.tableName
        }

    test("real YDB mirror restores linked data and batch cost price on a second device")
        .config(enabled = smokeEnabled) {
            val config = YdbMirrorJdbcConfig.fromEnvironment()
            val gateway = YdbJdbcMirrorSyncGateway(config)
            val source = createManagedTestDatabase()
            val target = createManagedTestDatabase()
            val prefix = "codex-device-smoke-${UUID.randomUUID()}"
            val initialAt = LocalDateTime(2026, 6, 11, 11, 0)
            val newerAt = LocalDateTime(2026, 6, 11, 11, 1)
            val deletedAt = LocalDateTime(2026, 6, 11, 11, 2)
            val rows = linkedRows(prefix, initialAt)
            val syncIds = rows.mapTo(mutableSetOf()) { it.row.syncId }

            try {
                val sourceApply = MirrorLocalApplyRepository(
                    source.database,
                    MirrorEntityApplyRepository(source.database),
                )
                sourceApply.apply(rows)

                val sourceSnapshot = MirrorLocalSnapshotRepository(source.database)
                    .loadSnapshot(LINKED_TABLES)
                    .onlySyncIds(syncIds)
                gateway.pushMirrorState(sourceSnapshot.toChanges()).getOrThrow()

                val remoteSnapshot = gateway.loadRemoteSnapshot(LINKED_TABLES)
                    .getOrThrow()
                    .onlySyncIds(syncIds)
                val targetApply = MirrorLocalApplyRepository(
                    target.database,
                    MirrorEntityApplyRepository(target.database),
                )
                targetApply.apply(remoteSnapshot.toChanges())

                val targetSnapshot = MirrorLocalSnapshotRepository(target.database)
                    .loadSnapshot(LINKED_TABLES)
                    .onlySyncIds(syncIds)
                LINKED_TABLES.forEach { table ->
                    targetSnapshot.rowsByTable.getValue(table).size shouldBe 1
                }
                target.database.batchCostDao
                    .getBySyncId("$prefix-batch")
                    ?.costPricePerUnit shouldBe 42_500L

                val newerVendor = (rows.single { it.table == MirrorSyncTable.VENDOR }.row as VendorMirrorRow)
                    .copy(displayName = "Device A newer vendor", updatedAt = newerAt)
                sourceApply.apply(
                    listOf(MirrorPushEntityChange(MirrorSyncTable.VENDOR, newerVendor))
                )

                val localNewer = MirrorLocalSnapshotRepository(source.database)
                    .loadSnapshot(LINKED_TABLES)
                    .onlySyncIds(syncIds)
                val plan = MirrorReconciliationPlanner().plan(localNewer, remoteSnapshot)
                gateway.pushMirrorState(plan.pushChanges).getOrThrow()

                val pulledVendor = gateway.loadRemoteSnapshot(listOf(MirrorSyncTable.VENDOR))
                    .getOrThrow()
                    .onlySyncIds(setOf("$prefix-vendor"))
                    .rowsByTable
                    .getValue(MirrorSyncTable.VENDOR)
                    .single() as VendorMirrorRow
                pulledVendor.displayName shouldBe newerVendor.displayName

                val tombstones = localNewer.toChanges().map { change ->
                    change.copy(row = change.row.markDeleted(deletedAt))
                }
                gateway.pushMirrorState(tombstones).getOrThrow()
                targetApply.apply(tombstones)

                val deletedTarget = MirrorLocalSnapshotRepository(target.database)
                    .loadDatabaseSnapshot(LINKED_TABLES)
                    .onlySyncIds(syncIds)
                val deletedRows = deletedTarget.rowsByTable.values.flatten()
                deletedRows.size shouldBe rows.size
                deletedRows.forEach { row ->
                    row.deletedAt shouldBe deletedAt
                }
                target.database.batchCostDao
                    .getBySyncId("$prefix-batch")
                    ?.deletedAt shouldBe deletedAt
            } finally {
                runCatching {
                    gateway.pushMirrorState(
                        rows.map { change ->
                            change.copy(row = change.row.markDeleted(deletedAt))
                        }
                    ).getOrThrow()
                }
                target.close()
                source.close()
            }
        }
})

private val LINKED_TABLES = listOf(
    MirrorSyncTable.VENDOR,
    MirrorSyncTable.DECLARATION,
    MirrorSyncTable.PRODUCT,
    MirrorSyncTable.BATCH,
    MirrorSyncTable.BATCH_COST_PRICE,
)

private fun linkedRows(
    prefix: String,
    updatedAt: LocalDateTime,
): List<MirrorPushEntityChange> = listOf(
    MirrorPushEntityChange(
        MirrorSyncTable.VENDOR,
        VendorMirrorRow(
            syncId = "$prefix-vendor",
            displayName = "Device A vendor",
            comment = "",
            updatedAt = updatedAt,
        ),
    ),
    MirrorPushEntityChange(
        MirrorSyncTable.DECLARATION,
        DeclarationMirrorRow(
            syncId = "$prefix-declaration",
            displayName = "Device A declaration",
            createdAt = LocalDate(2026, 6, 11),
            vendorSyncId = "$prefix-vendor",
            vendorName = "Device A vendor",
            bornDate = LocalDate(2026, 6, 1),
            bestBefore = LocalDate(2026, 12, 1),
            observeFromNotification = false,
            updatedAt = updatedAt,
        ),
    ),
    MirrorPushEntityChange(
        MirrorSyncTable.PRODUCT,
        ProductMirrorRow(
            syncId = "$prefix-product",
            type = ProductType.FOOD_BASE,
            displayName = "Device A product",
            secondName = "",
            createdAt = LocalDate(2026, 6, 11),
            comment = "",
            priceForSale = 50_000L,
            shelfLifeDays = 30,
            recNds = 20,
            updatedAt = updatedAt,
        ),
    ),
    MirrorPushEntityChange(
        MirrorSyncTable.BATCH,
        BatchMirrorRow(
            syncId = "$prefix-batch",
            productSyncId = "$prefix-product",
            dateBorn = LocalDate(2026, 6, 11),
            declarationSyncId = "$prefix-declaration",
            updatedAt = updatedAt,
        ),
    ),
    MirrorPushEntityChange(
        MirrorSyncTable.BATCH_COST_PRICE,
        BatchCostPriceMirrorRow(
            syncId = "$prefix-batch",
            batchSyncId = "$prefix-batch",
            costPricePerUnit = 42_500L,
            updatedAt = updatedAt,
        ),
    ),
)

private fun MirrorLocalSnapshot.onlySyncIds(syncIds: Set<String>) = copy(
    rowsByTable = rowsByTable.mapValues { (_, rows) ->
        rows.filter { it.syncId in syncIds }
    },
)

private fun MirrorRemoteSnapshot.onlySyncIds(syncIds: Set<String>) = copy(
    rowsByTable = rowsByTable.mapValues { (_, rows) ->
        rows.filter { it.syncId in syncIds }
    },
)

private fun MirrorLocalSnapshot.toChanges() = rowsByTable.flatMap { (table, rows) ->
    rows.map { row -> MirrorPushEntityChange(table, row) }
}

private fun MirrorRemoteSnapshot.toChanges() = rowsByTable.flatMap { (table, rows) ->
    rows.map { row -> MirrorPushEntityChange(table, row) }
}

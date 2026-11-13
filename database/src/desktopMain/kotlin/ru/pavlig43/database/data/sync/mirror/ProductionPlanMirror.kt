package ru.pavlig43.database.data.sync.mirror

import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.production.ProductionPlan
import ru.pavlig43.database.data.production.ProductionPlanRow

internal fun ProductionPlan.toMirrorRow() = ProductionPlanMirrorRow(syncId, title, createdAt, updatedAt, deletedAt)

internal suspend fun ProductionPlanRow.toMirrorRow(db: NocombroDatabase) = ProductionPlanItemMirrorRow(
    syncId = syncId,
    planSyncId = requireNotNull(db.productionPlanDao.getPlan(planId)).syncId,
    productSyncId = db.productDao.getProduct(productId).syncId,
    position = position,
    plannedGrams = plannedGrams,
    packageGrams = packageGrams,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal suspend fun NocombroDatabase.applyProductionPlan(change: MirrorPushEntityChange) {
    val row = change.row as ProductionPlanMirrorRow
    val existing = productionPlanDao.getPlanBySyncId(row.syncId)
    if (existing != null && (existing.deletedAt ?: existing.updatedAt) >= row.versionAt()) return
    if (row.deletedAt != null && existing == null) return
    productionPlanDao.upsertPlan(
        ProductionPlan(row.title, row.createdAt, existing?.id ?: 0, row.syncId, row.updatedAt, row.deletedAt)
    )
}

internal suspend fun NocombroDatabase.applyProductionPlanRow(change: MirrorPushEntityChange) {
    val row = change.row as ProductionPlanItemMirrorRow
    val existing = productionPlanDao.getRowBySyncId(row.syncId)
    if (existing != null && (existing.deletedAt ?: existing.updatedAt) >= row.versionAt()) return
    val incoming = if (row.deletedAt != null) {
        existing?.copy(updatedAt = row.updatedAt, deletedAt = row.deletedAt) ?: return
    } else {
        ProductionPlanRow(
            planId = checkNotNull(productionPlanDao.getPlanBySyncId(row.planSyncId)).id,
            productId = checkNotNull(productDao.getProductBySyncId(row.productSyncId)).id,
            position = row.position,
            plannedGrams = row.plannedGrams,
            packageGrams = row.packageGrams,
            id = existing?.id ?: 0,
            syncId = row.syncId,
            updatedAt = row.updatedAt,
        )
    }
    productionPlanDao.upsertRows(listOf(incoming))
}

internal object ProductionPlanYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.PRODUCTION_PLAN
    override val columnNames = listOf("sync_id", "title", "created_at", "updated_at", "deleted_at")

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ProductionPlanMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.title)
        statement.setString(3, row.createdAt.toString())
        statement.setString(4, row.updatedAt.toString())
        statement.setString(5, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ProductionPlanMirrorRow(
        syncId = resultSet.getString("sync_id"),
        title = resultSet.getString("title"),
        createdAt = LocalDateTime.parse(resultSet.getString("created_at")),
        updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
        deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
    )
}

internal object ProductionPlanItemYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.PRODUCTION_PLAN_ROW
    override val columnNames = listOf(
        "sync_id", "plan_sync_id", "product_sync_id", "row_position",
        "planned_grams", "package_grams", "updated_at", "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ProductionPlanItemMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.planSyncId)
        statement.setString(3, row.productSyncId)
        statement.setInt(4, row.position)
        statement.setLong(5, row.plannedGrams)
        statement.setLong(6, row.packageGrams)
        statement.setString(7, row.updatedAt.toString())
        statement.setString(8, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ProductionPlanItemMirrorRow(
        syncId = resultSet.getString("sync_id"),
        planSyncId = resultSet.getString("plan_sync_id"),
        productSyncId = resultSet.getString("product_sync_id"),
        position = resultSet.getInt("row_position"),
        plannedGrams = resultSet.getLong("planned_grams"),
        packageGrams = resultSet.getLong("package_grams"),
        updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
        deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
    )
}

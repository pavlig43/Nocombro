package ru.pavlig43.database.data.sync.mirror

import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.plan.ProductPlan
import ru.pavlig43.database.data.product.plan.ProductPlanRow

internal fun ProductPlan.toMirrorRow() = ProductPlanMirrorRow(syncId, createdAt, updatedAt, deletedAt)

internal suspend fun ProductPlanRow.toMirrorRow(db: NocombroDatabase) = ProductPlanItemMirrorRow(
    syncId = syncId,
    planSyncId = requireNotNull(db.productPlanDao.getPlan(planId)).syncId,
    productSyncId = db.productDao.getProduct(productId).syncId,
    position = position,
    plannedGrams = plannedGrams,
    packageGrams = packageGrams,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal suspend fun NocombroDatabase.applyProductPlan(change: MirrorPushEntityChange) {
    val row = change.row as ProductPlanMirrorRow
    val existing = productPlanDao.getPlanBySyncId(row.syncId)
    if (existing != null && (existing.deletedAt ?: existing.updatedAt) >= row.versionAt()) return
    if (row.deletedAt != null && existing == null) return
    productPlanDao.upsertPlan(
        ProductPlan(row.createdAt, existing?.id ?: 0, row.syncId, row.updatedAt, row.deletedAt)
    )
}

internal suspend fun NocombroDatabase.applyProductPlanRow(change: MirrorPushEntityChange) {
    val row = change.row as ProductPlanItemMirrorRow
    val existing = productPlanDao.getRowBySyncId(row.syncId)
    if (existing != null && (existing.deletedAt ?: existing.updatedAt) >= row.versionAt()) return
    val incoming = if (row.deletedAt != null) {
        existing?.copy(updatedAt = row.updatedAt, deletedAt = row.deletedAt) ?: return
    } else {
        ProductPlanRow(
            planId = checkNotNull(productPlanDao.getPlanBySyncId(row.planSyncId)).id,
            productId = checkNotNull(productDao.getProductBySyncId(row.productSyncId)).id,
            position = row.position,
            plannedGrams = row.plannedGrams,
            packageGrams = row.packageGrams,
            id = existing?.id ?: 0,
            syncId = row.syncId,
            updatedAt = row.updatedAt,
        )
    }
    productPlanDao.upsertRows(listOf(incoming))
}

internal object ProductPlanYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.PRODUCTION_PLAN
    override val columnNames = listOf("sync_id", "created_at", "updated_at", "deleted_at")

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ProductPlanMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.createdAt.toString())
        statement.setString(3, row.updatedAt.toString())
        statement.setString(4, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ProductPlanMirrorRow(
        syncId = resultSet.getString("sync_id"),
        createdAt = LocalDateTime.parse(resultSet.getString("created_at")),
        updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
        deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
    )
}

internal object ProductPlanItemYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.PRODUCTION_PLAN_ROW
    override val columnNames = listOf(
        "sync_id", "plan_sync_id", "product_sync_id", "row_position",
        "planned_grams", "package_grams", "updated_at", "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is ProductPlanItemMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.planSyncId)
        statement.setString(3, row.productSyncId)
        statement.setInt(4, row.position)
        statement.setLong(5, row.plannedGrams)
        statement.setLong(6, row.packageGrams)
        statement.setString(7, row.updatedAt.toString())
        statement.setString(8, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = ProductPlanItemMirrorRow(
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

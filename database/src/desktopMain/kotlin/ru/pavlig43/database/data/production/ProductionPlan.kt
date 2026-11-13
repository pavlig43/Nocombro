package ru.pavlig43.database.data.production

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val PRODUCTION_PLAN_TABLE = "production_plan"
const val PRODUCTION_PLAN_ROW_TABLE = "production_plan_row"

@Entity(tableName = PRODUCTION_PLAN_TABLE, indices = [Index(value = ["sync_id"], unique = true)])
data class ProductionPlan(
    val title: String,
    @ColumnInfo("created_at") val createdAt: LocalDateTime,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("sync_id") val syncId: String = defaultSyncId(),
    @ColumnInfo("updated_at") val updatedAt: LocalDateTime = defaultUpdatedAt(),
    @ColumnInfo("deleted_at") val deletedAt: LocalDateTime? = null,
)

@Entity(
    tableName = PRODUCTION_PLAN_ROW_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = ProductionPlan::class, parentColumns = ["id"], childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Product::class, parentColumns = ["id"], childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plan_id"), Index("product_id"), Index(value = ["sync_id"], unique = true)],
)
data class ProductionPlanRow(
    @ColumnInfo("plan_id") val planId: Int,
    @ColumnInfo("product_id") val productId: Int,
    val position: Int,
    @ColumnInfo("planned_grams") val plannedGrams: Long,
    @ColumnInfo("package_grams") val packageGrams: Long,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("sync_id") val syncId: String = defaultSyncId(),
    @ColumnInfo("updated_at") val updatedAt: LocalDateTime = defaultUpdatedAt(),
    @ColumnInfo("deleted_at") val deletedAt: LocalDateTime? = null,
)

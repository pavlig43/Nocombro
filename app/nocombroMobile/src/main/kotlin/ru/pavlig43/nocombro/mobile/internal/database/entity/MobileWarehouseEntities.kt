package ru.pavlig43.nocombro.mobile.internal.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(tableName = "mobile_warehouse_product")
data class MobileWarehouseProductEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_sync_id")
    val productSyncId: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "main_balance")
    val mainBalance: Long,
    @ColumnInfo(name = "experimental_balance")
    val experimentalBalance: Long,
)

@Entity(tableName = "mobile_warehouse_snapshot")
data class MobileWarehouseSnapshotEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

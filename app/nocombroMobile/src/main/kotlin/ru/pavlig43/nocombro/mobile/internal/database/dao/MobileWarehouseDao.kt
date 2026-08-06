package ru.pavlig43.nocombro.mobile.internal.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileWarehouseProductEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileWarehouseSnapshotEntity

@Dao
abstract class MobileWarehouseDao {
    @Query("SELECT * FROM mobile_warehouse_product")
    abstract fun observeProducts(): Flow<List<MobileWarehouseProductEntity>>

    @Query(
        "SELECT * FROM mobile_warehouse_snapshot " +
            "WHERE id = ${MobileWarehouseSnapshotEntity.SINGLETON_ID}"
    )
    abstract fun observeSnapshotMetadata(): Flow<MobileWarehouseSnapshotEntity?>

    @Query("SELECT * FROM mobile_warehouse_product")
    abstract suspend fun getProducts(): List<MobileWarehouseProductEntity>

    @Query(
        "SELECT * FROM mobile_warehouse_snapshot " +
            "WHERE id = ${MobileWarehouseSnapshotEntity.SINGLETON_ID}"
    )
    abstract suspend fun getSnapshotMetadata(): MobileWarehouseSnapshotEntity?

    @Query("DELETE FROM mobile_warehouse_product")
    abstract suspend fun deleteProducts()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertProducts(products: List<MobileWarehouseProductEntity>)

    @Upsert
    abstract suspend fun upsertSnapshotMetadata(snapshot: MobileWarehouseSnapshotEntity)

    @Transaction
    open suspend fun replaceSnapshot(
        products: List<MobileWarehouseProductEntity>,
        snapshot: MobileWarehouseSnapshotEntity,
    ) {
        deleteProducts()
        insertProducts(products)
        upsertSnapshotMetadata(snapshot)
    }
}

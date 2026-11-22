package ru.pavlig43.database.data.product.plan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductPlanDao {
    @Query("SELECT * FROM production_plan WHERE deleted_at IS NULL ORDER BY created_at DESC, id DESC")
    fun observePlans(): Flow<List<ProductPlan>>

    @Query("SELECT * FROM production_plan WHERE id = :id")
    suspend fun getPlan(id: Int): ProductPlan?

    @Query("SELECT * FROM production_plan WHERE sync_id = :syncId")
    suspend fun getPlanBySyncId(syncId: String): ProductPlan?

    @Query("SELECT * FROM production_plan")
    suspend fun getAllPlans(): List<ProductPlan>

    @Query("SELECT * FROM production_plan_row WHERE plan_id = :planId AND deleted_at IS NULL ORDER BY position, id")
    suspend fun getRows(planId: Int): List<ProductPlanRow>

    @Query("SELECT * FROM production_plan_row WHERE sync_id = :syncId")
    suspend fun getRowBySyncId(syncId: String): ProductPlanRow?

    @Query("SELECT * FROM production_plan_row")
    suspend fun getAllRows(): List<ProductPlanRow>

    @Insert
    suspend fun insertPlan(plan: ProductPlan): Long

    @Upsert
    suspend fun upsertPlan(plan: ProductPlan)

    @Upsert
    suspend fun upsertRows(rows: List<ProductPlanRow>)

    @Query("DELETE FROM production_plan WHERE id IN (:ids)")
    suspend fun deletePlansByIds(ids: Set<Int>)

    @Query("DELETE FROM production_plan_row WHERE id IN (:ids)")
    suspend fun deleteRows(ids: List<Int>)
}

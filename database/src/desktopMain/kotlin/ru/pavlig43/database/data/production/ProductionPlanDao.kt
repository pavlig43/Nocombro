package ru.pavlig43.database.data.production

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionPlanDao {
    @Query("SELECT * FROM production_plan WHERE deleted_at IS NULL ORDER BY created_at DESC, id DESC")
    fun observePlans(): Flow<List<ProductionPlan>>

    @Query("SELECT * FROM production_plan WHERE id = :id")
    suspend fun getPlan(id: Int): ProductionPlan?

    @Query("SELECT * FROM production_plan WHERE sync_id = :syncId")
    suspend fun getPlanBySyncId(syncId: String): ProductionPlan?

    @Query("SELECT * FROM production_plan")
    suspend fun getAllPlans(): List<ProductionPlan>

    @Query("SELECT * FROM production_plan_row WHERE plan_id = :planId AND deleted_at IS NULL ORDER BY position, id")
    suspend fun getRows(planId: Int): List<ProductionPlanRow>

    @Query("SELECT * FROM production_plan_row WHERE sync_id = :syncId")
    suspend fun getRowBySyncId(syncId: String): ProductionPlanRow?

    @Query("SELECT * FROM production_plan_row")
    suspend fun getAllRows(): List<ProductionPlanRow>

    @Insert
    suspend fun insertPlan(plan: ProductionPlan): Long

    @Upsert
    suspend fun upsertPlan(plan: ProductionPlan)

    @Upsert
    suspend fun upsertRows(rows: List<ProductionPlanRow>)

    @Query("DELETE FROM production_plan WHERE id = :id")
    suspend fun deletePlan(id: Int)

    @Query("DELETE FROM production_plan_row WHERE id IN (:ids)")
    suspend fun deleteRows(ids: List<Int>)
}

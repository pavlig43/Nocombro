package ru.pavlig43.database.data.vendor.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.common.IsCanUpsertResult
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME
import ru.pavlig43.database.data.vendor.Vendor

@Dao
interface VendorDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(vendor: Vendor): Long

    @Update
    suspend fun updateVendor(vendor: Vendor)

    @Query("DELETE FROM $VENDOR_TABLE_NAME WHERE id IN (:ids)")
    suspend fun deleteVendorsByIds(ids: Set<Int>)

    @Query("SELECT * from $VENDOR_TABLE_NAME WHERE id = :id")
    suspend fun getVendor(id: Int): Vendor

    @Query(
        """
    SELECT * FROM $VENDOR_TABLE_NAME
    """
    )
    fun observeOnVendors(): Flow<List<Vendor>>

    @Query(
        """
        SELECT CASE
            WHEN (SELECT display_name FROM $VENDOR_TABLE_NAME WHERE id =:id) =:name THEN TRUE
            ELSE NOT EXISTS (SELECT 1 FROM $VENDOR_TABLE_NAME WHERE display_name = :name AND id != :id)
        END
    """
    )
    suspend fun isNameAllowed(id: Int, name: String): Boolean

    suspend fun isCanSave(vendor: Vendor): IsCanUpsertResult{
        if (vendor.displayName.isBlank()) return IsCanUpsertResult.NameBlank()
        if (!isNameAllowed(vendor.id,vendor.displayName)) return IsCanUpsertResult.NameExists()
        return IsCanUpsertResult.Ok()
    }


}
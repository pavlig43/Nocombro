package ru.pavlig43.database.data.vendor.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.vendor.VendorFile

@Dao
interface VendorFilesDao {
    @Query("SELECT * FROM vendor_file WHERE vendor_id = :vendorId")
    suspend fun getFiles(vendorId: Int):List<VendorFile>

    @Upsert
    suspend fun upsertVendorFiles(files:List<VendorFile>)

    @Query("DELETE FROM vendor_file WHERE id in(:ids)")
    suspend fun deleteFiles(ids:List<Int>)

}
package ru.pavlig43.database.data.safety

import androidx.room.Dao
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SafetyTableDao {

    internal fun observeOnCountProductOnStorage(): Flow<StorageProductNow>{

    }
}
internal data class StorageProductNow(
    val productId: Int,
    val productName: String,
    val vendorName: String,
    val count: String
)
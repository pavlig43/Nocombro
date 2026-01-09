package ru.pavlig43.database.data.files

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Upsert
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.document.Document
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.transaction.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME


@Dao
abstract class FileDao {
    @Query("SELECT * FROM file WHERE owner_id = :ownerId AND owner_type =:ownerFileType")
    abstract suspend fun getFiles(ownerId: Int,ownerFileType: OwnerType):List<FileBD>

    @Upsert
    abstract suspend fun upsertFiles(files:List<FileBD>)

    @Query("DELETE FROM file WHERE id in(:ids)")
    abstract suspend fun deleteFiles(ids:List<Int>)





}
package ru.pavlig43.database.data.sync.mirror

import androidx.room.Dao
import androidx.room.Query
import ru.pavlig43.database.data.batch.BATCH_COST_PRICE_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_MOVEMENT_TABLE_NAME
import ru.pavlig43.database.data.batch.BATCH_TABLE_NAME
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_TABLE_NAME
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.product.COMPOSITION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_DECLARATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_SPECIFICATION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.SAFETY_STOCK_TABLE_NAME
import ru.pavlig43.database.data.transact.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.transact.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transact.reminder.REMINDER_TABLE_NAME
import ru.pavlig43.database.data.transact.sale.SALE_TABLE_NAME
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME

@Dao
@Suppress("TooManyFunctions")
interface MirrorHardDeleteDao {
    @Query("DELETE FROM $VENDOR_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteVendor(syncId: String): Int

    @Query("DELETE FROM $DOCUMENT_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteDocument(syncId: String): Int

    @Query("DELETE FROM $DECLARATIONS_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteDeclaration(syncId: String): Int

    @Query("DELETE FROM $PRODUCT_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteProduct(syncId: String): Int

    @Query("DELETE FROM $TRANSACTION_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteTransaction(syncId: String): Int

    @Query("DELETE FROM $EXPERIMENT_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteExperiment(syncId: String): Int

    @Query("DELETE FROM $PRODUCT_SPECIFICATION_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteProductSpecification(syncId: String): Int

    @Query("DELETE FROM $SAFETY_STOCK_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteSafetyStock(syncId: String): Int

    @Query("DELETE FROM $EXPERIMENT_ENTRY_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteExperimentEntry(syncId: String): Int

    @Query("DELETE FROM $EXPERIMENT_REMINDER_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteExperimentReminder(syncId: String): Int

    @Query("DELETE FROM $PRODUCT_DECLARATION_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteProductDeclaration(syncId: String): Int

    @Query("DELETE FROM $COMPOSITION_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteComposition(syncId: String): Int

    @Query("DELETE FROM $BATCH_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteBatch(syncId: String): Int

    @Query("DELETE FROM $BATCH_COST_PRICE_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteBatchCostPrice(syncId: String): Int

    @Query("DELETE FROM $BATCH_MOVEMENT_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteBatchMovement(syncId: String): Int

    @Query("DELETE FROM $REMINDER_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteReminder(syncId: String): Int

    @Query("DELETE FROM $EXPENSE_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteExpense(syncId: String): Int

    @Query("DELETE FROM $BUY_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteBuy(syncId: String): Int

    @Query("DELETE FROM $SALE_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteSale(syncId: String): Int

    @Query("DELETE FROM $FILE_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun deleteFile(syncId: String): Int
}

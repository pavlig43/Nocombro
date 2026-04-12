package ru.pavlig43.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.data.analytic.profitability.ProfitabilityDao
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.dao.BatchCostDao
import ru.pavlig43.database.data.batch.dao.BatchDao
import ru.pavlig43.database.data.batch.dao.BatchMovementDao
import ru.pavlig43.database.data.common.Converters
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.declaration.dao.DeclarationDao
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.dao.ExpenseDao
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.FileDao
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.database.data.product.dao.CompositionDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.product.dao.ProductSpecificationDao
import ru.pavlig43.database.data.product.dao.SafetyStockDao
import ru.pavlig43.database.data.safety.SafetyTableDao
import ru.pavlig43.database.data.storage.dao.StorageDao
import ru.pavlig43.database.data.sync.SyncChangeEntity
import ru.pavlig43.database.data.sync.SyncDao
import ru.pavlig43.database.data.sync.SyncStateEntity
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.dao.BuyDao
import ru.pavlig43.database.data.transact.dao.TransactionDao
import ru.pavlig43.database.data.transact.ingredient.dao.IngredientDao
import ru.pavlig43.database.data.transact.pf.dao.PfDao
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.reminder.dao.ReminderDao
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.transact.sale.dao.SaleDao
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.dao.VendorDao

@Database(
    entities = [
        FileBD::class,

        Document::class,

        Vendor::class,

        Declaration::class,

        Product::class,
        CompositionIn::class,
        ProductDeclarationIn::class,
        ProductSpecification::class,
        SafetyStock::class,

        BatchBD::class,
        BatchCostPriceEntity::class,

        BatchMovement::class,

        Transact::class,

        BuyBDIn::class,
        SaleBDIn::class,

        ReminderBD::class,

        ExpenseBD::class,

        SyncChangeEntity::class,
        SyncStateEntity::class,

    ],
    version = 4,

)
@TypeConverters(Converters::class)
abstract class NocombroDatabase : RoomDatabase() {

    abstract val fileDao: FileDao
    abstract val documentDao: DocumentDao

    abstract val vendorDao: VendorDao

    abstract val declarationDao: DeclarationDao

    abstract val productDao: ProductDao
    abstract val productDeclarationDao: ProductDeclarationDao
    abstract val productSpecificationDao: ProductSpecificationDao
    abstract val compositionDao: CompositionDao
    abstract val safetyStockDao: SafetyStockDao

    abstract val transactionDao: TransactionDao
    abstract val batchDao: BatchDao
    abstract val batchCostDao: BatchCostDao

    abstract val batchMovementDao: BatchMovementDao
    abstract val buyDao: BuyDao
    abstract val saleDao: SaleDao
    abstract val reminderDao: ReminderDao
    abstract val expenseDao: ExpenseDao
    abstract val pfDao: PfDao
    abstract val ingredientDao: IngredientDao

    abstract val storageDao: StorageDao

    abstract val safetyTableDao: SafetyTableDao

    abstract val profitabilityDao: ProfitabilityDao
    abstract val syncDao: SyncDao
}








class NocombroTransactionExecutor(
    private val db: NocombroDatabase
) : TransactionExecutor {

    override suspend fun transaction(blocks: List<suspend () -> Result<Unit>>): Result<Unit> {
        return runCatching {
            db.inTransaction {
                blocks.forEach { block ->
                    block().getOrThrow()
                }
            }
        }
    }
}

/**
 * Выполняет блок в writer-транзакции Room и возвращает его результат.
 */
suspend fun <T> NocombroDatabase.inTransaction(block: suspend () -> T): T {
    return useWriterConnection { transactor ->
        transactor.immediateTransaction {
            block()
        }
    }
}



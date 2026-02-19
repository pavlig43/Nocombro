package ru.pavlig43.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
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
import ru.pavlig43.database.data.product.dao.CompositionDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.dao.BuyDao
import ru.pavlig43.database.data.transact.dao.TransactionDao
import ru.pavlig43.database.data.transact.pf.dao.PfDao
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.reminder.dao.ReminderDao
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

        BatchBD::class,

        BatchMovement::class,

        Transact::class,

        BuyBDIn::class,

        ReminderBD::class,

        ExpenseBD::class,

    ],
    version = 1,


)
@TypeConverters(Converters::class)
@ConstructedBy(NocombroDatabaseConstructor::class)
abstract class NocombroDatabase : RoomDatabase() {

    abstract val fileDao: FileDao
    abstract val documentDao: DocumentDao

    abstract val vendorDao: VendorDao

    abstract val declarationDao: DeclarationDao

    abstract val productDao: ProductDao
    abstract val productDeclarationDao: ProductDeclarationDao
    abstract val compositionDao: CompositionDao

    abstract val transactionDao: TransactionDao
    abstract val batchDao: BatchDao

    abstract val batchMovementDao: BatchMovementDao
    abstract val buyDao: BuyDao
    abstract val reminderDao: ReminderDao
    abstract val expenseDao: ExpenseDao
    abstract val pfDao: PfDao
}


@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object NocombroDatabaseConstructor : RoomDatabaseConstructor<NocombroDatabase> {
    override fun initialize(): NocombroDatabase
}

fun getNocombroDatabase(builder: RoomDatabase.Builder<NocombroDatabase>): NocombroDatabase {
    val database = builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    CoroutineScope(Dispatchers.IO).launch {
        seedDatabase(database)
    }
    return database
}





class NocombroTransactionExecutor(
    private val db: NocombroDatabase
) : TransactionExecutor {

    override suspend fun transaction(blocks: List<suspend () -> Result<Unit>>): Result<Unit> {
        return runCatching {
            db.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    blocks.forEach { block ->
                            block().getOrThrow()
                        }
                    }
                }
            }
        }
    }




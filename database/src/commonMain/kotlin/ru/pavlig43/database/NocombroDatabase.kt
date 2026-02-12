package ru.pavlig43.database

import androidx.room.AutoMigration
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
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.emptyDate
import ru.pavlig43.database.data.batch.Batch
import ru.pavlig43.database.data.common.Converters
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.declaration.dao.DeclarationDao
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.FileDao
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.dao.CompositionDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.transact.BatchMovement
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionProductBDIn
import ru.pavlig43.database.data.transact.dao.TransactionDao
import ru.pavlig43.database.data.transact.expense.ExpenseBD
import ru.pavlig43.database.data.transact.expense.dao.ExpenseDao
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.dao.BuyDao
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.reminder.dao.ReminderDao
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.dao.VendorDao
import kotlin.time.ExperimentalTime

@Database(
    entities = [
        FileBD::class,

        Document::class,

        Vendor::class,

        Declaration::class,

        Product::class,
        CompositionIn::class,
        ProductDeclarationIn::class,

        Batch::class,

        Transact::class,
        BatchMovement::class,
        TransactionProductBDIn::class,
        BuyBDIn::class,

        ReminderBD::class,

        ExpenseBD::class,
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ]

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
    abstract val buyDao: BuyDao
    abstract val reminderDao: ReminderDao
    abstract val expenseDao: ExpenseDao
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
        initData(database)
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



@OptIn(ExperimentalTime::class)
@Suppress("LongMethod", "TooGenericExceptionCaught", "SwallowedException")
suspend fun initData(db: NocombroDatabase) {
    try {
        db.productDao.getProduct(1)
    } catch (_: Exception) {
        val products = listOf(
            Product(
                type = ProductType.FOOD_BASE,
                displayName = "Соль",
                createdAt = LocalDate.fromEpochDays(0),
                comment = "",
                id = 1
            ),
            Product(
                type = ProductType.FOOD_PF,
                displayName = "БАварские",
                createdAt = LocalDate.fromEpochDays(0),
                comment = "",
                id = 2
            ),
            Product(
                type = ProductType.FOOD_BASE,
                displayName = "Декстроза",
                createdAt = LocalDate.fromEpochDays(0),
                comment = "",
                id = 3
            )
        )
        val vendors = listOf(
            Vendor(
                displayName = "Ингре",
                comment = "",
                id = 1
            ),
            Vendor(
                displayName = "Стоинг",
                comment = "",
                id = 2
            ),
            Vendor(
                displayName = "Рустарк",
                comment = "",
                id = 3
            ),
        )
        val declaration = listOf(
            Declaration(
                displayName = "Декларация ингре",
                createdAt = LocalDate.fromEpochDays(0),
                vendorId = 1,
                vendorName = "Ингре",
                bestBefore = LocalDate.fromEpochDays(0),
                id = 1,
                observeFromNotification = true,
                bornDate = emptyDate
            ),
            Declaration(
                displayName = "Декларация стоинг",
                createdAt = LocalDate.fromEpochDays(0),
                vendorId = 2,
                vendorName = "Стоинг",
                bestBefore = LocalDate.fromEpochDays(0),
                id = 2,
                observeFromNotification = true,
                bornDate = emptyDate
            ),
            Declaration(
                displayName = "Декларация рустарк",
                createdAt = LocalDate.fromEpochDays(0),
                vendorId = 3,
                vendorName = "Рустарк",
                bestBefore = LocalDate.fromEpochDays(0),
                id = 3,
                observeFromNotification = true,
                bornDate = emptyDate
            )
        )
        val productDeclarationDeps = listOf(
            ProductDeclarationIn(
                productId = 2,
                declarationId = 1,
                id = 1
            ),
            ProductDeclarationIn(
                productId = 2,
                declarationId = 2,
                id = 2
            ),
            ProductDeclarationIn(
                productId = 2,
                declarationId = 3,
                id = 3
            ),

            )
        products.forEach {
            db.productDao.create(it)
        }
        vendors.forEach {
            db.vendorDao.create(it)
        }
        declaration.forEach {
            db.declarationDao.create(it)
        }

        db.productDeclarationDao.upsertProductDeclarations(productDeclarationDeps)

    }


}






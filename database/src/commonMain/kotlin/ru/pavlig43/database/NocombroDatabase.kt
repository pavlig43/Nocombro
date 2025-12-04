package ru.pavlig43.database

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.database.data.common.Converters
import ru.pavlig43.database.data.declaration.DeclarationFile
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.declaration.dao.DeclarationDao
import ru.pavlig43.database.data.declaration.dao.DeclarationFileDao
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFile
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.dao.DocumentFilesDao
import ru.pavlig43.database.data.product.*
import ru.pavlig43.database.data.product.dao.CompositionDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.product.dao.ProductFilesDao
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.database.data.transaction.TransactionRow
import ru.pavlig43.database.data.transaction.dao.ProductTransactionDao
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VendorFile
import ru.pavlig43.database.data.vendor.dao.VendorDao
import ru.pavlig43.database.data.vendor.dao.VendorFilesDao
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [
        Document::class,
        DocumentFile::class,

        Vendor::class,
        VendorFile::class,

        Declaration::class,
        DeclarationFile::class,

        Product::class,
        ProductFile::class,
        ProductDeclaration::class,
        ProductComposition::class,
        ProductIngredientIn::class,

//        ProductBatchIn::class,
//        ProductBatchOperation::class,
        TransactionRow::class,
        ProductTransaction::class,
    ],


    version = 1
)
@TypeConverters(Converters::class)
@ConstructedBy(NocombroDatabaseConstructor::class)
abstract class NocombroDatabase : RoomDatabase() {
    abstract val documentDao: DocumentDao
    abstract val documentFilesDao: DocumentFilesDao

    abstract val vendorDao: VendorDao
    abstract val vendorFilesDao: VendorFilesDao

    abstract val declarationDao: DeclarationDao
    abstract val declarationFilesDao: DeclarationFileDao

    abstract val productDao: ProductDao
    abstract val productFilesDao: ProductFilesDao
    abstract val productDeclarationDao: ProductDeclarationDao
    abstract val compositionDao: CompositionDao

    abstract val productTransactionDao:ProductTransactionDao
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

interface DataBaseTransaction {
    suspend fun transaction(blocks: List<suspend () -> RequestResult<Unit>>): RequestResult<Unit>
}

class NocombroTransaction(
    private val db: NocombroDatabase
) : DataBaseTransaction {
//    override suspend fun transaction(blocks: List<suspend () -> Unit>) {
//        db.useWriterConnection { transactor ->
//            transactor.immediateTransaction {
//                blocks.forEach { block ->
//                    block()
//                }
//            }
//        }
//    }
    override suspend fun transaction(blocks: List<suspend () -> RequestResult<Unit>>): RequestResult<Unit> {
        val errors = mutableListOf<String>()

        return try {
            db.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    blocks.forEach { block ->
                        val result = block()
                        if (result is RequestResult.Error) {
                            errors.add(result.message?:"какая то ошибка ")
                            return@immediateTransaction
                        }
                    }
                }
            }

            if (errors.isEmpty()) {
                RequestResult.Success(Unit)
            } else {
                RequestResult.Error(message = errors.joinToString("; "))
            }
        } catch (e: Exception) {
            RequestResult.Error(message = e.message ?: "Transaction failed")
        }
    }

}

@OptIn(ExperimentalTime::class)
@Suppress("LongMethod","TooGenericExceptionCaught","SwallowedException")
suspend fun initData(db: NocombroDatabase) {
    try {
        db.productDao.getProduct(1)
    } catch (_: Exception) {
        val products = listOf(
            Product(
                type = ProductType.BASE,
                displayName = "Соль",
                createdAt = Clock.System.now().toEpochMilliseconds(),
                comment = "",
                id = 1
            ),
            Product(
                type = ProductType.NOCOMBRO_SPICE,
                displayName = "БАварские",
                createdAt = Clock.System.now().toEpochMilliseconds(),
                comment = "",
                id = 2
            ),
            Product(
                type = ProductType.BASE,
                displayName = "Декстроза",
                createdAt = Clock.System.now().toEpochMilliseconds(),
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
                createdAt = Clock.System.now().toEpochMilliseconds(),
                vendorId = 1,
                vendorName = "Ингре",
                bestBefore = 0,
                id = 1,
                observeFromNotification = true
            ),
            Declaration(
                displayName = "Декларация стоинг",
                createdAt = Clock.System.now().toEpochMilliseconds(),
                vendorId = 2,
                vendorName = "Стоинг",
                bestBefore = 0,
                id = 2,
                observeFromNotification = true
            ),
            Declaration(
                displayName = "Декларация рустарк",
                createdAt = Clock.System.now().toEpochMilliseconds(),
                vendorId = 3,
                vendorName = "Рустарк",
                bestBefore = 0,
                id = 3,
                observeFromNotification = true
            )
        )
        val productDeclarationDeps = listOf(
            ProductDeclaration(
                productId = 2,
                declarationId = 1,
                id = 1
            ),
            ProductDeclaration(
                productId = 2,
                declarationId = 2,
                id = 2
            ),
            ProductDeclaration(
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






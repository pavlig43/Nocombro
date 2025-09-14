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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.pavlig43.database.data.common.Converters
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFile
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.dao.DocumentFilesDao
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductComposition
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductFile
import ru.pavlig43.database.data.product.ProductIngredientIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.product.dao.ProductFilesDao
import ru.pavlig43.database.data.product.dao.CompositionDao
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [
        Document::class,
        DocumentFile::class,

        Product::class,
        ProductFile::class,
        ProductDeclaration::class,
        ProductComposition::class,
        ProductIngredientIn::class,


    ],
    version = 1
)
@TypeConverters(Converters::class)
@ConstructedBy(NocombroDatabaseConstructor::class)
abstract class NocombroDatabase : RoomDatabase() {
    abstract val documentDao: DocumentDao
    abstract val documentFilesDao:DocumentFilesDao

    abstract val productDao: ProductDao
    abstract val productFilesDao: ProductFilesDao
    abstract val productDeclarationDao: ProductDeclarationDao
    abstract val compositionDao: CompositionDao
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
    CoroutineScope(Dispatchers.IO).launch{
        initData(database)
    }
    return database
}
interface DataBaseTransaction {
    suspend fun transaction(blocks: List<suspend ()->Unit>)
}

class NocombroTransaction(
    private val db: NocombroDatabase
):DataBaseTransaction {
    override suspend fun transaction(blocks: List< suspend () -> Unit>) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                coroutineScope {
                    blocks.map { block ->
                        async { block() }
                    }.awaitAll()
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
suspend fun initData(db: NocombroDatabase){
    val product1 = try {
        db.productDao.getProduct(1)
    }
    catch (e:Exception){
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
        products.forEach {
            db.productDao.create(it)
        }
    }


}






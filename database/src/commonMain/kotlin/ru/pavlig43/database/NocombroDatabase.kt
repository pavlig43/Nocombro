package ru.pavlig43.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.data.common.Converters
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.declaration.DeclarationDependencies
import ru.pavlig43.database.data.product.ProductDependencies
import ru.pavlig43.database.data.specification.Specification

@Database(
    entities = [
        Product::class,
        Document::class,
        ProductDependencies::class,
        Specification::class,
        DeclarationDependencies::class,
        DocumentFilePath::class,
    ],
    version = 1
)
@TypeConverters(Converters::class)
@ConstructedBy(NocombroDatabaseConstructor::class)
abstract class NocombroDatabase : RoomDatabase() {
    abstract val productDao: ProductDao
    abstract val documentDao: DocumentDao
}


@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object NocombroDatabaseConstructor : RoomDatabaseConstructor<NocombroDatabase> {
    override fun initialize(): NocombroDatabase
}

fun getNocombroDatabase(builder: RoomDatabase.Builder<NocombroDatabase>): NocombroDatabase {
    return  builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

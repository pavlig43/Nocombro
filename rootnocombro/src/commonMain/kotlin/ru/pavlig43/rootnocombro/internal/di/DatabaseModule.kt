package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.NocombroTransaction
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.dao.DocumentFilesDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.product.dao.ProductFilesDao
import ru.pavlig43.rootnocombro.api.IRootDependencies


internal fun getDatabaseModule(rootDependencies: IRootDependencies) = listOf(
    module {
        single<NocombroDatabase> { rootDependencies.database }
        single<DataBaseTransaction> { NocombroTransaction(get()) }

        single <DocumentDao> { get<NocombroDatabase>().documentDao }
        single<DocumentFilesDao> {get<NocombroDatabase>().documentFilesDao  }

        single <ProductDao> { get<NocombroDatabase>().productDao }
        single <ProductFilesDao> { get<NocombroDatabase>().productFilesDao }
        single <ProductDeclarationDao> { get<NocombroDatabase>().productDeclarationDao }

    }
)

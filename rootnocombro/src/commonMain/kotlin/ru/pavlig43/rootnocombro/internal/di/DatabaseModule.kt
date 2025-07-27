package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.rootnocombro.api.IRootDependencies


internal fun getDatabaseModule(rootDependencies: IRootDependencies) = listOf(
    module {
        single<NocombroDatabase> { rootDependencies.database }
        single <DocumentDao> { get<NocombroDatabase>().documentDao }
        single <ProductDao> { get<NocombroDatabase>().productDao }

    }
)

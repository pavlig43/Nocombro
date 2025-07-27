package ru.pavlig43.productlist.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.productlist.api.IProductLisDependencies

internal fun createModule(dependencies: IProductLisDependencies) = listOf(
    productListModule,
    module {
        single<ProductDao> { dependencies.productDao }
    }
)
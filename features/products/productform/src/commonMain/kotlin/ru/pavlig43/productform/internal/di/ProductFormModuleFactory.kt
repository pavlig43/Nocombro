package ru.pavlig43.productform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.productform.api.IProductFormDependencies

private fun baseModule(dependencies: IProductFormDependencies) = module {
    single<ProductDao> { dependencies.productDao }
}
internal fun createProductFormModule(dependencies: IProductFormDependencies) = listOf(
    baseModule(dependencies),
    createProductFormModule
)


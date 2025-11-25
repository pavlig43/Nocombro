package ru.pavlig43.transactionform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType
import ru.pavlig43.transactionform.ITransactionDependencies

private fun baseModule(dependencies: ITransactionDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
    single<IItemListRepository<Product, ProductType>> (named(ItemListType.Product.name)){ dependencies.productListRepository }
}
internal fun createTransactionFormModule(dependencies: ITransactionDependencies) = listOf(
    baseModule(dependencies),
    transactionFormModule
)
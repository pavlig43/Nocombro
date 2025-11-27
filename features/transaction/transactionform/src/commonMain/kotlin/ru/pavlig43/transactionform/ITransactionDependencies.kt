package ru.pavlig43.transactionform

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.itemlist.api.data.IItemListRepository

interface ITransactionDependencies {
    val db:NocombroDatabase
    val productListRepository: IItemListRepository<Product, ProductType>
}
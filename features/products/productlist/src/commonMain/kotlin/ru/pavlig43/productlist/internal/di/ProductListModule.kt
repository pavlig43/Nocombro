package ru.pavlig43.productlist.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListRepository

internal val productListModule = module {
    single<ItemListRepository<Product, ProductType>> { getProductListRepository(get()) }

}
private fun getProductListRepository(
    productDao: ProductDao
): ItemListRepository<Product, ProductType> {
    return ItemListRepository<Product,ProductType>(
        tag = "Product list Repository",
        deleteByIds = productDao::deleteProducts,
        observeAllItem = productDao::observeAllProducts,
        observeItemsByTypes = productDao::observeProductsByProductType
    )
}
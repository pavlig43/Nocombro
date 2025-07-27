package ru.pavlig43.productform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.productform.internal.ui.INIT_BASE_VALUES
import ru.pavlig43.productform.internal.ui.SAVE_REPOSITORY_TAG
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.internal.data.InitItemRepository
import ru.pavlig43.manageitem.api.data.RequireValues
import ru.pavlig43.upsertitem.data.ISaveItemRepository
import ru.pavlig43.upsertitem.data.SaveItemRepository


internal val createProductFormModule = module {
    single<ISaveItemRepository<Product>> { getSaveRepository(get()) }
    single<IInitDataRepository<Product,RequireValues>> { getInitRequireValuesRepository(get()) }
}
private fun getSaveRepository(
    productDao: ProductDao
): ISaveItemRepository<Product> {
    return SaveItemRepository(
        isNameExist = productDao::isNameExist,
        insertNewItem = productDao::insertProduct,
        updateItem = productDao::updateProduct,
        tag = SAVE_REPOSITORY_TAG
    )
}
private fun getInitRequireValuesRepository(
    productDao: ProductDao
): IInitDataRepository<Product,RequireValues> {

    return InitItemRepository<Product>(
        tag = INIT_BASE_VALUES,
        iniDataForState = RequireValues(),
        loadData = productDao::getProduct,

    )
}

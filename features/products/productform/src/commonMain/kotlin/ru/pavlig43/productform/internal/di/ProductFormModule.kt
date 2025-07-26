package ru.pavlig43.productform.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.dao.ProductDao

import ru.pavlig43.documentform.internal.ui.SAVE_PRODUCT_REPOSITORY
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.upsertitem.data.ISaveItemRepository
import ru.pavlig43.upsertitem.data.SaveItemRepository


internal val createDocumentFormModule = module {
    singleOf(::InitBaseValuesDocumentRepository) bind IInitDataRepository::class
    single<ISaveItemRepository<Product>> { getSaveRepository(get())}
}
private fun getSaveRepository(
    productDao: ProductDao
): ISaveItemRepository<Product> {
    return SaveItemRepository(
        isNameExist = productDao::isNameExist,
        insertNewItem = productDao::insertProduct,
        updateItem = productDao::updateProduct,
        tag = SAVE_PRODUCT_REPOSITORY
    )
}
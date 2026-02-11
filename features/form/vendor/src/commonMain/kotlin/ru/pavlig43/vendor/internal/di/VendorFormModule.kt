package ru.pavlig43.vendor.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.create.data.CreateSingleItemRepository
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.update.data.UpdateSingleLineRepository
import ru.pavlig43.vendor.api.VendorFormDependencies

internal fun createVendorFormModule(dependencies: VendorFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateSingleItemRepository<Vendor>> { getCreateRepository(get()) }
        single<UpdateSingleLineRepository<Vendor>> { getUpdateRepository(get()) }

    }
)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateSingleItemRepository<Vendor> {
    val dao = db.vendorDao
    return CreateSingleItemRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateSingleLineRepository<Vendor> {
    val dao = db.vendorDao
    return UpdateSingleLineRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getVendor,
        updateItem = dao::updateVendor
    )
}







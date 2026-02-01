package ru.pavlig43.vendor.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.update.data.UpdateEssentialsRepository
import ru.pavlig43.vendor.api.VendorFormDependencies

internal fun createVendorFormModule(dependencies: VendorFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateEssentialsRepository<Vendor>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Vendor>> { getUpdateRepository(get()) }

    }
)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Vendor> {
    val dao = db.vendorDao
    return CreateEssentialsRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Vendor> {
    val dao = db.vendorDao
    return UpdateEssentialsRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getVendor,
        updateItem = dao::updateVendor
    )
}







package ru.pavlig43.vendor.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VendorFile
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.api.data.UpdateEssentialsRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository
import ru.pavlig43.vendor.api.VendorFormDependencies

internal fun createVendorFormModule(dependencies: VendorFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<CreateEssentialsRepository<Vendor>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Vendor>> { getUpdateRepository(get()) }

        single<UpdateCollectionRepository<VendorFile, VendorFile>>(named(UpdateCollectionRepositoryType.Files.name)) {
            getFilesRepository(
                get()
            )
        }
    }
)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Vendor> {
    val dao = db.vendorDao
    return CreateEssentialsRepository(
        tag = "Create Vendor Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Vendor> {
    val dao = db.vendorDao
    return UpdateEssentialsRepository(
        tag = "Update vendor sRepository",
        isNameAllowed = dao::isNameAllowed,
        loadItem = dao::getVendor,
        updateItem = dao::updateVendor
    )
}

internal enum class UpdateCollectionRepositoryType {
    Files,
}


private fun getFilesRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<VendorFile, VendorFile> {
    val fileDao = db.vendorFilesDao
    return UpdateCollectionRepository(
        tag = "Vendor FilesRepository",
        loadCollection = fileDao::getFiles,
        deleteCollection = fileDao::deleteFiles,
        upsertCollection = fileDao::upsertVendorFiles
    )
}


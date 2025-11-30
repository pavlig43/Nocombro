package ru.pavlig43.vendor.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VendorFile
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.manageitem.api.UpsertEssentialsDependencies
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository
import ru.pavlig43.vendor.api.VendorFormDependencies

internal fun createVendorFormModule(dependencies: VendorFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<UpsertEssentialsDependencies> { dependencies.upsertEssentialsDependencies }
//        single<UpdateItemRepository<Vendor>> { getCreateRepository(get()) }
        single<IUpdateRepository<Vendor, Vendor>>(named(UpdateRepositoryType.Vendor.name)) {
            getInitItemRepository(
                get()
            )
        }

        single<UpdateCollectionRepository<VendorFile, VendorFile>>(named(UpdateCollectionRepositoryType.Files.name)) {
            getFilesRepository(
                get()
            )
        }
    }
)

//private fun getCreateRepository(
//    db: NocombroDatabase
//): UpdateItemRepository<Vendor> {
//    val dao = db.vendorDao
//    return UpdateItemRepository(
//        tag = "Create Vendor Repository",
//        isNameAllowed = dao::isNameAllowed,
//        create = dao::create
//    )
//}

internal enum class UpdateRepositoryType {
    Vendor,
}

internal enum class UpdateCollectionRepositoryType {
    Files,
}

private fun getInitItemRepository(
    db: NocombroDatabase
): IUpdateRepository<Vendor, Vendor> {
    val dao = db.vendorDao
    return UpdateItemRepository<Vendor>(
        tag = "Update Vendor Repository",
        loadItem = dao::getVendor,
        updateItem = dao::updateVendor
    )
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


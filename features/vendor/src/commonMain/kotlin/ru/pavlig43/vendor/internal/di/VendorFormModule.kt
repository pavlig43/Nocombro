package ru.pavlig43.vendor.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VendorFile
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

internal val vendorFormModule = module {

    single<CreateItemRepository<Vendor>> { getCreateRepository(get()) }
    single<IUpdateRepository<Vendor>>(named(UpdateRepositoryType.Vendor.name)) {
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

private fun getCreateRepository(
    db: NocombroDatabase
): CreateItemRepository<Vendor> {
    val dao = db.vendorDao
    return CreateItemRepository(
        tag = "Create Vendor Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create
    )
}

internal enum class UpdateRepositoryType {
    Vendor,
}

internal enum class UpdateCollectionRepositoryType {
    Files,
    Declaration,
}

private fun getInitItemRepository(
    db: NocombroDatabase
): IUpdateRepository<Vendor> {
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


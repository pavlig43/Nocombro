package ru.pavlig43.vendor.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.vendor.api.VendorFormDependencies

internal fun createVendorFormModule(dependencies: VendorFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateSingleItemRepository<Vendor>> { getCreateRepository(get()) }
        single<UpdateSingleLineRepository<Vendor>> { VendorUpdateRepository(get()) }

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

private class VendorUpdateRepository(
    private val db: NocombroDatabase
) : UpdateSingleLineRepository<Vendor> {

    private val dao = db.vendorDao

    override suspend fun getInit(id: Int): Result<Vendor> {
        return runCatching {
            dao.getVendor(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<Vendor>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            dao.isCanSave(changeSet.new).getOrThrow()
            dao.updateVendor(changeSet.new)
        }
    }
}








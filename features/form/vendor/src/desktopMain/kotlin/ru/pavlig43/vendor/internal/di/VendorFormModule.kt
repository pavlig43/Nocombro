package ru.pavlig43.vendor.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.inTransaction
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.TransactionalCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.TransactionalUpdateSingleLineRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.vendor.api.VendorFormDependencies

internal fun createVendorFormModule(dependencies: VendorFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateSingleItemRepository<Vendor>> { VendorCreateRepository(get()) }
        single<UpdateSingleLineRepository<Vendor>> { VendorUpdateRepository(get()) }

    }
)

private class VendorCreateRepository(
    db: NocombroDatabase,
) : TransactionalCreateSingleItemRepository<Vendor>(
    inWriteTransaction = { block -> db.inTransaction(block) },
) {
    private val dao = db.vendorDao

    override suspend fun validate(item: Vendor): Result<Unit> = dao.isCanSave(item)

    override suspend fun createInDb(item: Vendor): Int = dao.create(item).toInt()
}

private class VendorUpdateRepository(
    db: NocombroDatabase,
) : TransactionalUpdateSingleLineRepository<Vendor>(
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.vendorDao

    override suspend fun getInit(id: Int): Result<Vendor> {
        return runCatching {
            dao.getVendor(id)
        }
    }

    override fun prepareForUpdate(item: Vendor): Vendor = item.copy(updatedAt = defaultUpdatedAt())

    override suspend fun validate(item: Vendor): Result<Unit> = dao.isCanSave(item)

    override suspend fun updateInDb(item: Vendor) {
        dao.updateVendor(item)
    }
}








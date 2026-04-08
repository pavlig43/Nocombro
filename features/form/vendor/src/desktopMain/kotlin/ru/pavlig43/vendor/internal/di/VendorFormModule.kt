package ru.pavlig43.vendor.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME
import ru.pavlig43.database.inTransaction
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncUpdateSingleLineRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.vendor.api.VendorFormDependencies

internal fun createVendorFormModule(dependencies: VendorFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateSingleItemRepository<Vendor>> { VendorCreateRepository(get(), get()) }
        single<UpdateSingleLineRepository<Vendor>> { VendorUpdateRepository(get(), get()) }

    }
)

private class VendorCreateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncCreateSingleItemRepository<Vendor>(
    tableName = VENDOR_TABLE_NAME,
    enqueueUpsert = syncQueueRepository::enqueueUpsert,
    runInTransaction = { block -> db.inTransaction(block) },
) {
    private val dao = db.vendorDao

    override suspend fun validate(item: Vendor): Result<Unit> = dao.isCanSave(item)

    override suspend fun createInDb(item: Vendor): Int = dao.create(item).toInt()
}

private class VendorUpdateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateSingleLineRepository<Vendor>(
    tableName = VENDOR_TABLE_NAME,
    enqueueUpsert = syncQueueRepository::enqueueUpsert,
    runInTransaction = { block -> db.inTransaction(block) },
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








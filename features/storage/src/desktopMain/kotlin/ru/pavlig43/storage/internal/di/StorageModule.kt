package ru.pavlig43.storage.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.storage.BatchMovementWithBalanceInfoBD
import ru.pavlig43.database.data.storage.StorageOperationPreview
import ru.pavlig43.database.data.storage.StorageOperationsRepository
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.database.data.storage.StorageTransferRequest
import ru.pavlig43.database.data.storage.StorageWriteOffRequest
import ru.pavlig43.storage.api.StorageDependencies

internal fun createStorageModule(dependencies: StorageDependencies) = listOf(module {
    single<NocombroDatabase> { dependencies.db }
    single { StorageRepository(get()) }
})

class StorageRepository(
    db: NocombroDatabase
) {
    private val dao = db.storageDao
    private val operations = StorageOperationsRepository(db)

    fun observeOnStorageProducts(
        start: LocalDateTime,
        end: LocalDateTime,
        storageLocation: StorageLocation = StorageLocation.MAIN,
    ): Flow<Result<List<StorageProduct>>> {
        return dao.observeOnStorageProduct(
            start = start,
            end = end,
            storageLocation = storageLocation,
        ).map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    fun observeBatchMovementsWithBalance(
        batchId: Int,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<Result<BatchMovementWithBalanceInfoBD>> {
        return dao.observeBatchMovementsWithBalance(batchId, start, end)
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    suspend fun previewOperation(
        batchId: Int,
        source: StorageLocation,
        count: Long,
    ): Result<StorageOperationPreview> = runCatching {
        operations.preview(batchId, source, count)
    }

    suspend fun transfer(request: StorageTransferRequest): Result<Unit> {
        return operations.transfer(request)
    }

    suspend fun writeOff(request: StorageWriteOffRequest): Result<Unit> {
        return operations.writeOff(request)
    }
}

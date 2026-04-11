package ru.pavlig43.storage.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.storage.BatchMovementWithBalanceInfoBD
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.storage.api.StorageDependencies

internal fun createStorageModule(dependencies: StorageDependencies) = listOf(module {
    single<NocombroDatabase> { dependencies.db }
    single { StorageRepository(get()) }
})

class StorageRepository(
    db: NocombroDatabase
) {
    private val dao = db.storageDao

    fun observeOnStorageProducts(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<Result<List<StorageProduct>>> {
        return dao.observeOnStorageProduct(
            start = start,
            end = end
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
}

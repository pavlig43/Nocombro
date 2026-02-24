package ru.pavlig43.storage.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.storage.api.StorageDependencies

internal fun createStorageModule(dependencies: StorageDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
}
class StorageRepository(
    db: NocombroDatabase
){
    private val dao = db.batchMovementDao

    fun observeOnStorageProducts(): Flow<Result<List<String>>> {
        return  dao.observeOnStorageProducts().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}
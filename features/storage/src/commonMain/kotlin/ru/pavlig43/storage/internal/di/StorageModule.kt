package ru.pavlig43.storage.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.koin.dsl.module
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.core.getCurrentLocalDateTime
import ru.pavlig43.core.minusMonths
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.storage.api.StorageDependencies

internal fun createStorageModule(dependencies: StorageDependencies) = listOf(module {
    single<NocombroDatabase> { dependencies.db }
    single { StorageRepository(get()) }
})
class StorageRepository(
    db: NocombroDatabase
){
    private val dao = db.storageDao

    fun observeOnStorageProducts(): Flow<Result<List<String>>> {
        return  dao.observeOnStorageBatches(
            start = getCurrentLocalDateTime().minusMonths(5,TimeZone.currentSystemDefault()),
            end = getCurrentLocalDateTime()
        ).map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}
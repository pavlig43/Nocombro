package ru.pavlig43.itemlist.internal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map


internal class ImmutableListRepository<I>(
    private val delete: suspend (Set<Int>) -> Unit,
    private val observe: () -> Flow<List<I>>,
) {
    suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { delete(ids) }

    }

    fun observeOnItems(
    ): Flow<Result<List<I>>> {
        return observe().map {
            Result.success(it)
        }
            .catch { emit(Result.failure(it)) }


    }
}
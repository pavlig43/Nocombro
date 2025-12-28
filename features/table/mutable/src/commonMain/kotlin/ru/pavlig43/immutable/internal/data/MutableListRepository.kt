package ru.pavlig43.immutable.internal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map


internal class MutableListRepository<I>(
    private val delete: suspend (Set<Int>) -> Unit,
    private val observe: () -> Flow<List<I>>,
) {


    fun observeOnItems(
    ): Flow<Result<List<I>>> {
        return observe().map {
            Result.success(it)
        }
            .catch { emit(Result.failure(it)) }


    }
}
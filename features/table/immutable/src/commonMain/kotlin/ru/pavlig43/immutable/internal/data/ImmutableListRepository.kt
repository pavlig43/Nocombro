package ru.pavlig43.immutable.internal.data

import kotlinx.coroutines.flow.Flow

internal interface ImmutableListRepository<I> {
    suspend fun deleteByIds(ids: Set<Int>): Result<Unit>

    fun observeOnItems(): Flow<Result<List<I>>>
}
package ru.pavlig43.flowImmutable.api.data

import kotlinx.coroutines.flow.Flow

interface FlowMultilineRepository<Out,In> {

    suspend fun getInit(parentId: Int): Result<List<In>>
    fun observeOnItemsByIds(ids: List<Int>): Flow<Result<List<Out>>>

}
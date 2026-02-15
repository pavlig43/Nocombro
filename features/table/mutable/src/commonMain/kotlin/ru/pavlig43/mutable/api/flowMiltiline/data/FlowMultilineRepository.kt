package ru.pavlig43.mutable.api.flowMiltiline.data

import kotlinx.coroutines.flow.Flow

interface FlowMultilineRepository<BD> {

    suspend fun getInit(parentId: Int): Result<List<Int>>
    fun observeOnItemsByIds(ids: List<Int>): Flow<Result<List<BD>>>

}
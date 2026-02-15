package ru.pavlig43.mutable.api.flowMiltiline.data

import kotlinx.coroutines.flow.Flow

interface FlowMultilineRepository<I> {

    fun observeOnItemsByIds(ids: List<Int>): Flow<Result<List<I>>>

}
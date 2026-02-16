package ru.pavlig43.flowImmutable.api.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.model.ChangeSet

interface FlowMultilineRepository<BdOut, BdIn> {

    suspend fun getInit(parentId: Int): Result<List<BdIn>>
    fun observeOnItemsByIds(ids: List<Int>): Flow<Result<List<BdOut>>>

    suspend fun update(changeSet: ChangeSet<List<BdIn>>): Result<Unit>

}
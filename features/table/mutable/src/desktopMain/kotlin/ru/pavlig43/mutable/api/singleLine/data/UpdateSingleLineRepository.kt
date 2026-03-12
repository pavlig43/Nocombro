package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem

interface UpdateSingleLineRepository<I : SingleItem> {
    suspend fun getInit(id: Int): Result<I>
    suspend fun update(changeSet: ChangeSet<I>): Result<Unit>
}
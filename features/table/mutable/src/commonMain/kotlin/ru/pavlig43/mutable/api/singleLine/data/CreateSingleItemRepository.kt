package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.SingleItem

interface CreateSingleItemRepository<I : SingleItem> {
    suspend fun createEssential(item: I): Result<Int>
}

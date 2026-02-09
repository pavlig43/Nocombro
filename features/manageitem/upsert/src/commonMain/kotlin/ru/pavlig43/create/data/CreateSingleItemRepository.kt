package ru.pavlig43.create.data

import ru.pavlig43.core.model.GenericItem

class CreateSingleItemRepository<I : GenericItem>(
    private val isCanSave: suspend (I) -> Result<Unit>,
    private val create: suspend (I) -> Long,
) {


    suspend fun createEssential(item: I): Result<Int> {
        return runCatching {
            isCanSave(item).getOrThrow()
            create(item).toInt()
        }

    }

}

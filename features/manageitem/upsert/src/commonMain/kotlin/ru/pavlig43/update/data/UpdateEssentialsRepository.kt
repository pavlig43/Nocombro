package ru.pavlig43.update.data

import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericItem

class UpdateEssentialsRepository<I : GenericItem>(
    private val isCanSave: suspend (I) -> Result<Unit>,
    private val loadItem: suspend (Int) -> I,
    private val updateItem: suspend (I) -> Unit,
)  {


    suspend fun getInit(id: Int): Result<I> {
        return runCatching {
            loadItem(id)
        }
    }


    suspend fun update(changeSet: ChangeSet<I>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            isCanSave(changeSet.new).getOrThrow()
            updateItem(changeSet.new)
        }

    }
}
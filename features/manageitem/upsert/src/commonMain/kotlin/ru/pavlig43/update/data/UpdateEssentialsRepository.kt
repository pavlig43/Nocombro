package ru.pavlig43.update.data

import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.database.data.common.IsCanUpsertResult

class UpdateEssentialsRepository<I : GenericItem>(
    private val isCanSave: suspend (I) -> IsCanUpsertResult,
    private val loadItem: suspend (Int) -> I,
    private val updateItem: suspend (I) -> Unit,
)  {

    private suspend fun isNameAllowed(
        item: I
    ): Result<String> {
        return runCatching { isCanSave(item) }
            .fold(
                onSuccess = { isCan ->
                    when (isCan) {
                        is IsCanUpsertResult.Ok -> {
                            Result.success(isCan.message)
                        }

                        else -> Result.failure(IllegalArgumentException(isCan.message))
                    }
                },
                onFailure = { Result.failure(it) }
            )


    }


    suspend fun getInit(id: Int): Result<I> {
        return runCatching {
            loadItem(id)
        }
    }


    suspend fun update(changeSet: ChangeSet<I>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            isNameAllowed(changeSet.new).getOrThrow()
            updateItem(changeSet.new)
        }

    }
}
package ru.pavlig43.create.data

import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.database.data.common.IsCanUpsertResult

class CreateEssentialsRepository<I : GenericItem>(
    private val isCanSave: suspend (I) -> IsCanUpsertResult,
    private val create: suspend (I) -> Long,
) {
    private suspend fun isCanSaveResult(
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




    suspend fun createEssential(item: I): Result<Int> {
        return runCatching {
            isCanSaveResult(item).getOrThrow()
            create(item).toInt()
        }

    }

}

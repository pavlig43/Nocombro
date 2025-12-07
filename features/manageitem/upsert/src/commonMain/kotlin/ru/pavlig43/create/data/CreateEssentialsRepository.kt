package ru.pavlig43.create.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.common.IsCanUpsertResult

class CreateEssentialsRepository<I : GenericItem>(
    private val tag: String,
    private val isCanSave: suspend (I) -> IsCanUpsertResult,
    private val create: suspend (I) -> Long,
) {
    private suspend fun isCanSave(
        item: I,
        createItem: suspend (I) -> RequestResult<Int>
    ): RequestResult<Int> {
        val isCanUpsertResult = dbSafeCall(tag) {
            isCanSave(item)
        }.mapTo { isCanCreateResult ->
            when(isCanCreateResult){
                is IsCanUpsertResult.Ok -> ""
                else -> isCanCreateResult.message
            }
        }
        return if (isCanUpsertResult is RequestResult.Success){
            if (!isCanUpsertResult.data.isBlank()) RequestResult.Error<Int>(message = isCanUpsertResult.data)
            else createItem(item)
        } else {
            RequestResult.Error(message = isCanUpsertResult.data)
        }
    }


    suspend fun createEssential(item: I): RequestResult<Int> {
        return isCanSave(item) {
            dbSafeCall(tag) {
                create(item)
            }.mapTo { it.toInt() }
        }
    }

}

package ru.pavlig43.update.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.common.IsCanUpsertResult

class UpdateEssentialsRepository<I : GenericItem>(
    private val tag: String,
    private val isCanSave: suspend (I) -> IsCanUpsertResult,
    private val loadItem: suspend (Int) -> I,
    private val updateItem: suspend (I) -> Unit,
)  {
    private suspend fun isNameAllowed(
        item: I,
        update: suspend (I) -> RequestResult<Unit>
    ): RequestResult<Unit> {

        val isCanUpdateResult = dbSafeCall(tag) {
            isCanSave(item)
        }.mapTo { isCanUpdate ->
            when(isCanUpdate){
                is IsCanUpsertResult.Ok -> ""
                else -> isCanUpdate.message
            }
        }
        return if (isCanUpdateResult is RequestResult.Success){
            if (!isCanUpdateResult.data.isBlank()) RequestResult.Error<Unit>(message = isCanUpdateResult.data)
            else update(item)
        } else {
            RequestResult.Error(message = isCanUpdateResult.data)
        }
    }

    suspend fun getInit(id: Int): RequestResult<I> {
        return dbSafeCall(tag) {
            loadItem(id)
        }
    }


    suspend fun update(changeSet: ChangeSet<I>): RequestResult<Unit> {
        if (changeSet.old == changeSet.new) return RequestResult.Success(Unit)
        return isNameAllowed(changeSet.new) {
            dbSafeCall(tag) {
                updateItem(changeSet.new)
            }
        }

    }
}
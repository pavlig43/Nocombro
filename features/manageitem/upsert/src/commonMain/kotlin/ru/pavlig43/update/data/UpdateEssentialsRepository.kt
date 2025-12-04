package ru.pavlig43.update.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.mapTo

class UpdateEssentialsRepository<I : GenericItem>(
    private val tag: String,
    private val isNameAllowed: suspend (id: Int, name: String) -> Boolean,
    private val loadItem: suspend (Int) -> I,
    private val updateItem: suspend (I) -> Unit,
)  {
    private suspend fun isNameAllowed(
        item: I,
        update: suspend (I) -> RequestResult<Unit>
    ): RequestResult<Unit> {
        if (item.displayName.isBlank()) return RequestResult.Error(message = "Имя не должно быть пустым")
        val isNameAllowed = dbSafeCall(tag) {
            isNameAllowed(item.id, item.displayName)
        }

        return if (isNameAllowed is RequestResult.Success) {
            if (!isNameAllowed.data) RequestResult.Error(message = "Имя уже существует")
            else {
                update(item)
            }
        }
        else{
            isNameAllowed.mapTo {  }
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
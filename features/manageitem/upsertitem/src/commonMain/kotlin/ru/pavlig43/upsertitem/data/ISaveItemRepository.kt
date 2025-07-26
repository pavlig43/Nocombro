package ru.pavlig43.upsertitem.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.mapTo
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.upsertitem.internal.NAME_ALREADY_EXISTS_MESSAGE
import ru.pavlig43.upsertitem.internal.NAME_IS_EMPTY_MESSAGE

interface ISaveItemRepository<I: Item> {
    suspend fun saveItem(newItem: I, initItem: I): RequestResult<Unit>
}

 class SaveItemRepository<I:Item>(
    private val isNameExist:suspend (String)->Boolean,
    private val insertNewItem:suspend (I) -> Unit,
    private val updateItem:suspend (newItem:I,initItem:I)->Unit,
    private val tag:String,
) : ISaveItemRepository<I> {
    override suspend fun saveItem(
        newItem: I,
        initItem: I,
    ): RequestResult<Unit> {

        val nameIsExistResult: RequestResult<Unit> = getIsNameExistResult(newItem.displayName,initItem.displayName)

        if (nameIsExistResult is RequestResult.Error) return nameIsExistResult

        return if (newItem.id == 0) {
            saveNewItem(newItem)
        } else {
            update(newItem, initItem)
        }

    }

    private suspend fun getIsNameExistResult(newName: String, initName: String): RequestResult<Unit> {

        if (newName.isBlank()) return RequestResult.Error(message = NAME_IS_EMPTY_MESSAGE)

        val nameIsExistResult: RequestResult<Unit> =
            if (newName == initName) {
                RequestResult.Success(Unit)
            } else {
                dbSafeCall(tag) {
                    isNameExist(newName)
                }.let {
                    if (it.data == true) RequestResult.Error(message = NAME_ALREADY_EXISTS_MESSAGE) else it.mapTo { Unit }
                }
            }
        return nameIsExistResult
    }

    private suspend fun saveNewItem(newItem: I): RequestResult<Unit> {
        return dbSafeCall(tag) {
            insertNewItem(newItem)
        }

    }

    private suspend fun update(
        newItem: I,
        initItem: I
    ): RequestResult<Unit> {
        return dbSafeCall(tag) {
            updateItem(newItem, initItem)
        }
    }
}





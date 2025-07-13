package ru.pavlig43.createitem.api.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.createitem.internal.ui.NAME_ALREADY_EXISTS_MESSAGE
import ru.pavlig43.createitem.internal.ui.NAME_IS_EMPTY_MESSAGE
import ru.pavlig43.database.data.common.data.Item
import ru.pavlig43.database.data.common.data.ItemType

interface IItemFormRepository<I:Item,S:ItemType> {

    suspend fun isValidName(name:String): ValidNameResult
    suspend fun saveItem(item: I):RequestResult<Unit>

}
sealed class ValidNameResult{
    class Valid:ValidNameResult()
    class Empty(val message:String = NAME_IS_EMPTY_MESSAGE):ValidNameResult()
    class Error(val message:String):ValidNameResult()
    class AllReadyExists(val message:String = NAME_ALREADY_EXISTS_MESSAGE):ValidNameResult()
}


package ru.pavlig43.manageitem.api.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.mapTo

class CreateItemRepository<I : Item>(
    private val tag: String,
    private val isNameAllowed: suspend (id: Int, name: String) -> Boolean,
    private val create: suspend (I) -> Long
) {
    /**
     * Проверяется есть ли имя, если есть возвра
     */

    suspend fun createItem(item: I): RequestResult<Int> {
        if (item.displayName.isBlank()) return RequestResult.Error(message = "Имя не должно быть пустым")
        val isNameAllowed = dbSafeCall(tag){
            isNameAllowed(item.id,item.displayName)
        }
            .mapTo { isAllowed-> if (isAllowed) 1 else 0 }


        return if (isNameAllowed is RequestResult.Success) {
            if (isNameAllowed.data == 0) RequestResult.Error(message = "Имя уже существует")
            else {
                dbSafeCall(tag) {
                    create(item)
                }.mapTo { it.toInt() }
            }
        } else {
            isNameAllowed
        }
    }
}
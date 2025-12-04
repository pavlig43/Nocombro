package ru.pavlig43.create.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.mapTo

class CreateEssentialsRepository<I : GenericItem>(
    private val tag: String,
    private val isNameAllowed: suspend (id: Int, name: String) -> Boolean,
    private val create:suspend (I) -> Long,
) {
    private suspend fun isNameAllowed(
        item: I,
        upsert: suspend (I) -> RequestResult<Int>
    ): RequestResult<Int> {
        if (item.displayName.isBlank()) return RequestResult.Error(message = "Имя не должно быть пустым")
        val isNameAllowed = dbSafeCall(tag) {
            isNameAllowed(item.id, item.displayName)
        }
            .mapTo { isAllowed -> if (isAllowed) 1 else -1 }
        return if (isNameAllowed is RequestResult.Success) {
            if (isNameAllowed.data == -1) RequestResult.Error(message = "Имя уже существует")
            else {
                upsert(item)

            }
        } else {
            isNameAllowed
        }
    }



    suspend fun createEssential(item: I): RequestResult<Int> {
        return isNameAllowed(item) {
            dbSafeCall(tag) {
                create(item)
            }.mapTo { it.toInt() }
        }
    }


}
package ru.pavlig43.itemlist.api.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.RequestResult
import ru.pavlig43.database.data.common.data.Item
import ru.pavlig43.database.data.common.data.ItemType

interface IItemListRepository<I:Item,U:ItemUi,S:ItemType> {


    suspend fun deleteItemsById(ids: List<Int>): RequestResult<Unit>

    fun getAllItem(): Flow<RequestResult<List<I>>>

    fun getItemsByTypes(types: List<S>): Flow<RequestResult<List<I>>>

    fun getAllItemTypes():List<S>

    fun toItemUi(item: I): U
}
package ru.pavlig43.itemlist.api.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.RequestResult
import ru.pavlig43.database.data.common.data.Item
import ru.pavlig43.database.data.common.data.ItemType

interface IItemRepository<I:Item,U:ItemUi,S:ItemType> {

    suspend fun insert(item: I): RequestResult<Unit>

    suspend fun update(item: I): RequestResult<Int>

    suspend fun deleteItemsById(ids: List<Int>): RequestResult<Int>

    suspend fun getItem(id: Int): RequestResult<I>

    fun getAllItem(): Flow<RequestResult<List<I>>>

    fun getItemsByTypes(types: List<S>): Flow<RequestResult<List<I>>>

    fun getAllItemTypes():List<S>

    fun toItemUi(item: I): U
}
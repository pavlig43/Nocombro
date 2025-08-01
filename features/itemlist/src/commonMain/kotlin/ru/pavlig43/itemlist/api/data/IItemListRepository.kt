package ru.pavlig43.itemlist.api.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.core.mapTo

class ItemListRepository<I:Item, S: ItemType>(
    private val tag:String,
    private val deleteByIds:suspend (List<Int>)->Unit,
    private val observeAllItem:()->Flow<List<I>>,
    private val observeItemsByTypes:(types:List<S>)->Flow<List<I>>,
) {

    suspend fun deleteItemsById(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag){
            deleteByIds(ids)
        }
    }

    fun getAllItem(): Flow<RequestResult<List<ItemUi>>> {
        return dbSafeFlow(tag){
            observeAllItem()
        }.toFlowItemUi()
    }

    fun getItemsByTypes(types: List<S>): Flow<RequestResult<List<ItemUi>>> {
        return dbSafeFlow(tag){
            observeItemsByTypes(types)
        }.toFlowItemUi()
    }
    private fun I.toItemUi(): ItemUi {
        return ItemUi(
            id = id,
            displayName = displayName,
            type = type,
            createdAt = createdAt.convertToDateTime()
        )
    }
    private fun Flow<RequestResult<List<I>>>.toFlowItemUi(): Flow<RequestResult<List<ItemUi>>> {
       return this.map { result-> result.mapTo { list-> list.map { item-> item.toItemUi() } } }
    }



}

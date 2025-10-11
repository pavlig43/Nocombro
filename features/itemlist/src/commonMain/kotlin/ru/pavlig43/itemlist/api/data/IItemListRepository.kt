package ru.pavlig43.itemlist.api.data

import androidx.room.RoomRawQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.core.mapTo

abstract class IItemListRepository<I : Item, S : ItemType>(
    private val tableName: String,
    private val deleteByIds: suspend (List<Int>) -> Unit,
    private val observeOnItems: (query: RoomRawQuery) -> Flow<List<I>>
) {
    private val tag = "$tableName list repository"

    suspend fun deleteItemsById(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tableName) {
            deleteByIds(ids)
        }
    }
    protected abstract fun createFilterQuery(filter: ItemFilter<S>):RoomRawQuery

    fun observeItemsByFilter(
        filter: ItemFilter<S>
    ): Flow<RequestResult<List<ItemUi>>> {
        val query = createFilterQuery(filter)

        return dbSafeFlow(tag){observeOnItems(query)} .toFlowItemUi()
    }

    private fun I.toItemUi(): ItemUi {
        return ItemUi(
            id = id,
            displayName = displayName,
            type = type,
            createdAt = createdAt.convertToDateTime(),
            comment = comment
        )
    }

    private fun Flow<RequestResult<List<I>>>.toFlowItemUi(): Flow<RequestResult<List<ItemUi>>> {
        return this.map { result -> result.mapTo { list -> list.map { item -> item.toItemUi() } } }
    }


}

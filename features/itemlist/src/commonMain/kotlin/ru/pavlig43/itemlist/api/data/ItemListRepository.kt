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

class ItemListRepository<I : Item, S : ItemType>(
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

    fun observeItemsByFilter(
        types: List<S>,
        searchText: String,
    ): Flow<RequestResult<List<ItemUi>>> {
        val query = createFilterQuery(
            types = types,
            isFilterByTypes = types.isNotEmpty(),
            searchText = searchText,
            isFilterByText = searchText.isNotBlank()
        )

        return dbSafeFlow(tag){observeOnItems(query)} .toFlowItemUi()
    }
    private fun createFilterQuery(
        types: List<S>,
        isFilterByTypes: Boolean,
        searchText: String,
        isFilterByText: Boolean
    ): RoomRawQuery {
        val sql = """
        SELECT * FROM $tableName
        WHERE (
            (? = false OR type IN (${types.indices.joinToString { "?" }}))
            AND (
                ? = false 
                OR display_name LIKE '%' || ? || '%'
                OR comment LIKE '%' || ? || '%'
            )
        )
        ORDER BY id DESC
    """.trimIndent()
        return RoomRawQuery(
            sql = sql,

            onBindStatement = { statement ->
                var index = 1

                // isFilterByTypes
                statement.bindLong(index++, if (isFilterByTypes) 1 else 0)

                // Список types
                types.forEach { type ->
                    statement.bindText(index++, type.bdName)
                }

                // isFilterByText
                statement.bindLong(index++, if (isFilterByText) 1 else 0)

                // searchName
                statement.bindText(index++, searchText)

                //searchComment
                statement.bindText(index++,searchText)
            }
        )
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

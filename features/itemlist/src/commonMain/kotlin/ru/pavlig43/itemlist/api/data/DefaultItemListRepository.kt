package ru.pavlig43.itemlist.api.data

import androidx.room.RoomRawQuery
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType

class DefaultItemListRepository<I : Item, S : ItemType>(
    private val tableName: String,
    deleteByIds: suspend (List<Int>) -> Unit,
    observeOnItems: (query: RoomRawQuery) -> Flow<List<I>>
) : IItemListRepository<I, S>(
    tableName = tableName,
    deleteByIds = deleteByIds,
    observeOnItems = observeOnItems
) {
    override fun createFilterQuery(filter: ItemFilter<S>): RoomRawQuery {
        val f = filter as DefaultItemFilter<S>
        val types = f.types
        val searchText = f.searchText
        val isFilterByTypes = types.isNotEmpty()
        val isFilterByText = searchText.isNotBlank()
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
                statement.bindText(index++, searchText)
            }
        )

    }


}

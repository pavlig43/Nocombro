package ru.pavlig43.declarationlist.internal.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.data.declaration.DeclarationIn

class DeclarationListRepository(
    private val deleteByIds: suspend (List<Int>) -> Unit,
    private val observeOnItems: (searchText: String,isFilterByText: Boolean) -> Flow<List<DeclarationIn>>
) {
    private val tag = "Declaration list  list repository"

    suspend fun deleteItemsById(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            deleteByIds(ids)
        }
    }


    fun observeDeclarationByFilter(
        filter: DeclarationFilter,
    ): Flow<RequestResult<List<DeclarationIn>>> {

        return dbSafeFlow(tag){observeOnItems(filter.searchText,filter.isFilterByText)}
    }

}
data class DeclarationFilter(
    val searchText: String,
    val isFilterByText: Boolean,
)

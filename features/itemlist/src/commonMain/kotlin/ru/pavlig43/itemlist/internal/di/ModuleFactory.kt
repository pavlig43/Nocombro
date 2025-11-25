package ru.pavlig43.itemlist.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.component.refactoring.IItemListDependencies
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListRepository1


internal fun createItemListFormModule(dependencies: IItemListDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        factoryOf(::DocumentListRepository)
    }
)

internal class DocumentListRepository(
    db: NocombroDatabase
) {
    private val dao = db.documentDao
    private val tag = "DocumentListRepository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteDocumentsByIds(ids)
        }
    }

    fun observeOnItems(
        searchText: String,
        types: List<DocumentType>,
    ): Flow<RequestResult<List<Document>>> {
        return dbSafeFlow(tag) {
            dao.observeOnDocuments(
                searchText = searchText,
                types = types
            )
        }
    }
}





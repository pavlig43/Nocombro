package ru.pavlig43.itemlist.statik.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.data.dbSafeFlow
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transaction.OperationType
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.core.component.ValueFilterComponent
import ru.pavlig43.itemlist.statik.api.TransactionListParamProvider

internal class TransactionStaticListContainer(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    paramProvider: TransactionListParamProvider,
    private val listRepository: TransactionListRepository,

    ) : ComponentContext by componentContext,
    IStaticListContainer<ProductTransaction, TransactionItemUi> {

    val typeFilterComponent = ValueFilterComponent(
        componentContext = childContext("types"),
        initialValue = paramProvider.fullListTransactionTypes
    )

    val searchTextFilterComponent = ValueFilterComponent(
        componentContext = childContext("search_text"),
        initialValue = ""
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val listFlow: Flow<RequestResult<List<ProductTransaction>>> = combine(
        typeFilterComponent.valueFlow,
        searchTextFilterComponent.valueFlow
    ) { types, text ->
        listRepository.observeOnItems(
            searchText = text,
            types = types
        )
    }.flatMapLatest { it }


    override val staticListComponent: StaticListComponent<ProductTransaction, TransactionItemUi> =
        StaticListComponent(
            componentContext = childContext("body"),
            dataFlow = listFlow,
            deleteItemsById = listRepository::deleteByIds,
            searchTextComponent = searchTextFilterComponent,
            onCreate = onCreate,
            withCheckbox = paramProvider.withCheckbox,
            onItemClick = onItemClick,
            mapper = {this.toUi()},
        )


}

data class TransactionItemUi(

    val transactionType: TransactionType,

    val operationType: OperationType,

    val createdAt: Long,

    val comment: String,

    val isCompleted: Boolean,

    override val id: Int = 0,


) : IItemUi

private fun ProductTransaction.toUi(): TransactionItemUi {
    return TransactionItemUi(
        id = id,
        createdAt = createdAt,
        transactionType = transactionType,
        operationType = operationType,
        comment = comment,
        isCompleted = isCompleted,
    )
}

internal class TransactionListRepository(
    db: NocombroDatabase
) {
    private val dao = db.productTransactionDao
    private val tag = "TransactionListRepository"

    suspend fun deleteByIds(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(tag) {
            dao.deleteProductTransactionsByIds(ids)
        }
    }

    fun observeOnItems(
        searchText: String,
        types: List<TransactionType>,
    ): Flow<RequestResult<List<ProductTransaction>>> {
        return dbSafeFlow(tag) {
            dao.observeOnProductTransactions(
                searchText = searchText,
                types = types
            )
        }
    }
}
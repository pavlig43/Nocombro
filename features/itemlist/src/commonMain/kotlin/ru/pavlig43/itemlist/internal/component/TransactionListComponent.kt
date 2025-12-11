package ru.pavlig43.itemlist.internal.component

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
import ru.pavlig43.itemlist.api.TransactionListParamProvider
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.generateComponent

internal class TransactionListComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (IItemUi) -> Unit,
    paramProvider: TransactionListParamProvider,
    private val listRepository: TransactionListRepository,

    ) : ComponentContext by componentContext,
    IListComponent<ProductTransaction, TransactionItemUi> {

    val typeComponent = generateComponent(ItemFilter.Type(paramProvider.fullListTransactionTypes))

    val searchTextComponent = generateComponent(ItemFilter.SearchText(""))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val listFlow: Flow<RequestResult<List<ProductTransaction>>> = combine(
        typeComponent.filterFlow,
        searchTextComponent.filterFlow
    ) { types: ItemFilter.Type<TransactionType>, text: ItemFilter.SearchText ->
        listRepository.observeOnItems(
            searchText = text.value,
            types = types.value
        )
    }.flatMapLatest { it }


    override val staticItemsBodyComponent: StaticItemsBodyComponent<ProductTransaction, TransactionItemUi> =
        StaticItemsBodyComponent(
            componentContext = childContext("body"),
            dataFlow = listFlow,
            deleteItemsById = listRepository::deleteByIds,
            searchText = searchTextComponent.filterFlow,
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


) : IItemUi{
    override val composeKey: Int = id
}

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
package ru.pavlig43.itemlist.internal.component.items.transaction

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.itemlist.api.component.TransactionBuilder
import ru.pavlig43.itemlist.internal.component.ImmutableTableComponent
import ru.pavlig43.itemlist.internal.data.ImmutableListRepository
import ru.pavlig43.itemlist.internal.model.TableData
import ua.wwind.table.ColumnSpec


internal class TransactionTableComponent(
    componentContext: ComponentContext,
    tableBuilder: TransactionBuilder,
    onCreate: () -> Unit,
    onItemClick: (TransactionTableUi) -> Unit,
    repository: ImmutableListRepository<Transaction>,
) : ImmutableTableComponent<Transaction, TransactionTableUi, TransactionField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = TransactionFilterMatcher,
    sortMatcher = TransactionSorter,
    repository = repository,
) {

    override val columns: ImmutableList<ColumnSpec<TransactionTableUi, TransactionField, TableData<TransactionTableUi>>> =
        createTransactionColumn(
            onCreate = onCreate,
            listTypeForFilter = tableBuilder.fullListTransactionTypes,
            onEvent = ::onEvent
        )

}

private fun Transaction.toUi(): TransactionTableUi {
    return TransactionTableUi(
        composeId = id,
        createdAt = createdAt,
        transactionType = transactionType,
        comment = comment,
        isCompleted = isCompleted,
    )
}

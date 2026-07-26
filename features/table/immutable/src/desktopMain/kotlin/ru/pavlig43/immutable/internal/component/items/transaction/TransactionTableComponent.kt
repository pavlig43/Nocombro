package ru.pavlig43.immutable.internal.component.items.transaction

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.api.component.TransactionImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec


internal class TransactionTableComponent(
    componentContext: ComponentContext,
    tableBuilder: TransactionImmutableTableBuilder,
    onItemClick: (TransactionTableUi) -> Unit,
    onCreate: () -> Unit,
    repository: ImmutableListRepository<TransactionTableItem>,
) : ImmutableTableComponent<TransactionTableItem, TransactionTableUi, TransactionField>(
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
            listTypeForFilter = tableBuilder.fullListTransactionTypes,
            onEvent = ::onEvent
        )

    override val exportConfiguration: TableExportConfiguration<TransactionTableUi, TransactionField> =
        TableExportConfiguration(
            suggestedFileName = "transactions-export",
        )

}

private fun TransactionTableItem.toUi(): TransactionTableUi {
    return TransactionTableUi(
        composeId = transaction.id,
        createdAt = transaction.createdAt,
        transactionType = transaction.transactionType,
        counterpartyNames = counterpartyNames,
        comment = transaction.comment,
        isCompleted = transaction.isCompleted,
    )
}

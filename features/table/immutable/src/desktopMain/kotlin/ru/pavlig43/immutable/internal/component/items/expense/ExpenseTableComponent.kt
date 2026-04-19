package ru.pavlig43.immutable.internal.component.items.expense

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.database.data.expense.MainExpenseBD
import ru.pavlig43.immutable.api.component.ExpenseImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class ExpenseTableComponent(
    componentContext: ComponentContext,
    tableBuilder: ExpenseImmutableTableBuilder,
    onItemClick: (ExpenseTableUi) -> Unit,
    onCreate: () -> Unit,
    repository: ImmutableListRepository<MainExpenseBD>,
) : ImmutableTableComponent<MainExpenseBD, ExpenseTableUi, ExpenseField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = MainExpenseBD::toUi,
    filterMatcher = ExpenseFilterMatcher,
    sortMatcher = ExpenseSorter,
    repository = repository,
) {
    override val columns: ImmutableList<ColumnSpec<ExpenseTableUi, ExpenseField, TableData<ExpenseTableUi>>> =
        createExpenseColumn(::onEvent)

    override val exportConfiguration: TableExportConfiguration<ExpenseTableUi, ExpenseField> =
        TableExportConfiguration(
            suggestedFileName = "expenses-export",
        )
}

private fun MainExpenseBD.toUi(): ExpenseTableUi {
    return ExpenseTableUi(
        composeId = expense.id,
        expenseType = expense.expenseType,
        amount = DecimalData2(expense.amount),
        expenseDateTime = expense.expenseDateTime,
        comment = expense.comment,
        transactionId = transaction?.id
    )
}

package ru.pavlig43.transaction.internal.update.tabs.component.expenses

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.emptyLocalDateTime
import ru.pavlig43.database.data.transaction.expense.ExpenseBD
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class ExpensesComponent(
    componentContext: ComponentContext,
    private val transactionId: Int,
    repository: UpdateCollectionRepository<ExpenseBD, ExpenseBD>,
    transactionDateTimeFlow: Flow<LocalDateTime>,
) : MutableTableComponent<ExpenseBD, ExpenseBD, ExpensesUi, ExpensesField>(
    componentContext = componentContext,
    parentId = transactionId,
    title = "Расходы",
    sortMatcher = ExpensesSorter,
    filterMatcher = ExpensesFilterMatcher,
    repository = repository
) {

    override val columns: ImmutableList<ColumnSpec<ExpensesUi, ExpensesField, TableData<ExpensesUi>>> =
        createExpensesColumns(
            onEvent = ::onEvent
        )

    private val dateTime = transactionDateTimeFlow
        .stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        emptyLocalDateTime
    )
    init {
        coroutineScope.launch {
            transactionDateTimeFlow.collect { println(it) }
        }
    }
    override fun createNewItem(composeId: Int): ExpensesUi {
        return ExpensesUi(
            composeId = composeId,
            id = 0,
            transactionId = transactionId,
            expenseType = null,
            amount = 0,
            expenseDateTime = dateTime.value,
            comment = ""
        )
    }

    override fun ExpenseBD.toUi(composeId: Int): ExpensesUi {
        return ExpensesUi(
            composeId = composeId,
            id = id,
            transactionId = transactionId,
            expenseType = expenseType,
            amount = amount,
            expenseDateTime = expenseDateTime,
            comment = comment
        )
    }

    override fun ExpensesUi.toBDIn(): ExpenseBD {
        return ExpenseBD(
            transactionId = transactionId,
            expenseType = expenseType?:throw IllegalArgumentException("Такой ошибки не может быть"),
            amount = amount,
            expenseDateTime = expenseDateTime,
            comment = comment,
            id = id
        )
    }

    override val errorMessages: Flow<List<String>> = itemList.map { lst ->
        buildList {
            lst.forEach { expenseUi ->
                val place = "В строке ${expenseUi.composeId + 1}"
                if (expenseUi.amount == 0) add("$place сумма равна 0")
                if (expenseUi.expenseType == null) add("$place не выбран тип расхода")
            }
        }
    }
}

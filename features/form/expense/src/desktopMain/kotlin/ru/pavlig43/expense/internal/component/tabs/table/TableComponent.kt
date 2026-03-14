package ru.pavlig43.expense.internal.component.tabs.table

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.datetime.single.datetime.DateTimeComponent
import ru.pavlig43.expense.api.model.ExpenseStandaloneUi
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class TableComponent(
    componentContext: ComponentContext,
    repository: UpdateCollectionRepository<ExpenseBD, ExpenseBD>
) : MutableTableComponent<ExpenseBD, ExpenseBD, ExpenseStandaloneUi, ExpenseStandaloneField>(
    componentContext = componentContext,
    parentId = 0,
    title = "Расходы",
    sortMatcher = TableSorter,
    filterMatcher = TableFilterMatcher,
    repository = repository
) {
    private val dialogNavigation = SlotNavigation<ExpenseDialogChild>()

    val dialog = childSlot(
        source = dialogNavigation,
        key = "expense_dialog",
        serializer = ExpenseDialogChild.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )
    private val _dateTime =
        MutableStateFlow(itemList.value.firstOrNull()?.expenseDateTime ?: getCurrentLocalDateTime())
    val datetime = _dateTime.asStateFlow()

    private fun createDialogChild(
        dialogConfig: ExpenseDialogChild,
        context: ComponentContext
    ): DialogChild {
        return when (dialogConfig) {
            is ExpenseDialogChild.DateTimePicker -> {
                val dateTimeComponent = DateTimeComponent(
                    componentContext = context,
                    initDatetime = _dateTime.value,
                    onDismissRequest = { dialogNavigation.dismiss() },
                    onChangeDate = { new ->
                        _dateTime.update { new }
                    }
                )
                DialogChild.DateTime(dateTimeComponent)
            }
        }
    }
    fun openDateTimeDialog() = dialogNavigation.activate(ExpenseDialogChild.DateTimePicker)

    override val columns: ImmutableList<ColumnSpec<ExpenseStandaloneUi, ExpenseStandaloneField, TableData<ExpenseStandaloneUi>>> =
        createExpenseStandaloneColumns(
            onEvent = ::onEvent
        )

    override fun createNewItem(composeId: Int): ExpenseStandaloneUi {
        return ExpenseStandaloneUi(
            composeId = composeId,
            id = 0,
            expenseType = null,
            amount = DecimalData(0, DecimalFormat.Decimal2),
            expenseDateTime = getCurrentLocalDateTime(),
            comment = ""
        )
    }

    override fun ExpenseBD.toUi(composeId: Int): ExpenseStandaloneUi {
        return ExpenseStandaloneUi(
            composeId = composeId,
            id = id,
            expenseType = expenseType,
            amount = DecimalData(amount, DecimalFormat.Decimal2),
            expenseDateTime = expenseDateTime,
            comment = comment
        )
    }

    override fun ExpenseStandaloneUi.toBDIn(): ExpenseBD {
        return ExpenseBD(
            transactionId = null,
            expenseType = expenseType ?: throw IllegalArgumentException("Тип расхода обязателен"),
            amount = amount.value,
            expenseDateTime = _dateTime.value,
            comment = comment,
            id = id
        )
    }

    override val errorMessages: Flow<List<String>> = itemList.map { lst ->
        buildList {
            lst.forEach { expenseUi ->
                val place = "В строке ${expenseUi.composeId + 1}"
                if (expenseUi.amount.value == 0) add("$place сумма равна 0")
                if (expenseUi.expenseType == null) add("$place не выбран тип расхода")
            }
        }
    }
}

@Serializable
internal sealed interface ExpenseDialogChild {
    @Serializable
    data object DateTimePicker : ExpenseDialogChild
}

sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}

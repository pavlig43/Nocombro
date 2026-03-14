package ru.pavlig43.expense.internal.component.tabs.table

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.datetime.single.datetime.DateTimeComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec

private fun factory() = SingleLineComponentFactory<ExpenseBD, ExpenseUi>(
    initItem = ExpenseUi(),
    errorFactory = { expenseUi ->
        buildList { ->
            if (expenseUi.amount.value == 0) add("сумма равна 0")
            if (expenseUi.expenseType == null) add(" не выбран тип расхода")
        }
    },
    mapperToUi = { toUi() }
)

private fun ExpenseBD.toUi(): ExpenseUi {
    return ExpenseUi(
        id = id,
        expenseType = expenseType,
        amount = DecimalData(amount, DecimalFormat.Decimal2),
        expenseDateTime = expenseDateTime,
        comment = comment
    )
}

internal class TableComponent(
    componentContext: ComponentContext,
    expenseId: Int,
    repository: UpdateSingleLineRepository<ExpenseBD>
) : UpdateSingleLineComponent<ExpenseBD, ExpenseUi, ExpenseField>(
    componentContext = componentContext,
    id = expenseId,
    updateSingleLineRepository = repository,
    mapperToDTO = { this.toDTO() },
    componentFactory = factory()

) {
    private val dialogNavigation = SlotNavigation<ExpenseDialogChild>()

    val dialog = childSlot(
        source = dialogNavigation,
        key = "expense_dialog",
        serializer = ExpenseDialogChild.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    private fun createDialogChild(
        dialogConfig: ExpenseDialogChild,
        context: ComponentContext
    ): DialogChild {
        return when (dialogConfig) {
            is ExpenseDialogChild.DateTimePicker -> {
                val dateTimeComponent = DateTimeComponent(
                    componentContext = context,
                    initDatetime = item.value.expenseDateTime,
                    onDismissRequest = { dialogNavigation.dismiss() },
                    onChangeDate = { new ->
                        onChangeItem { it.copy(expenseDateTime = new) }
                    }
                )
                DialogChild.DateTime(dateTimeComponent)
            }
        }
    }

    override val columns: ImmutableList<ColumnSpec<ExpenseUi, ExpenseField, Unit>> =
        createExpenseColumns(
            openDateTimeDialog = { dialogNavigation.activate(ExpenseDialogChild.DateTimePicker) },
            onChangeItem = ::onChangeItem
        )
    override val errorMessages: Flow<List<String>> = errorTableMessages

}


@Serializable
internal sealed interface ExpenseDialogChild {
    @Serializable
    data object DateTimePicker : ExpenseDialogChild
}

sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}

private fun ExpenseUi.toDTO(): ExpenseBD {
    return ExpenseBD(
        transactionId = null,
        expenseType = expenseType ?: throw IllegalArgumentException("Тип расхода обязателен"),
        amount = amount.value,
        expenseDateTime = expenseDateTime,
        comment = comment,
        id = id
    )
}

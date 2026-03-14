package ru.pavlig43.expense.internal.update.tabs.essential

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.datetime.single.dateTime.DateTimeComponent
import ru.pavlig43.expense.internal.ExpenseField
import ru.pavlig43.expense.internal.model.toDto
import ru.pavlig43.expense.internal.model.ExpenseEssentialsUi
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec

internal class ExpenseUpdateSingleLineComponent(
    componentContext: ComponentContext,
    expenseId: Int,
    updateRepository: UpdateSingleLineRepository<ExpenseBD>,
    componentFactory: SingleLineComponentFactory<ExpenseBD, ExpenseEssentialsUi>,
    observeOnItem: (ExpenseEssentialsUi) -> Unit,
    onSuccessInitData: (ExpenseEssentialsUi) -> Unit,
) : UpdateSingleLineComponent<ExpenseBD, ExpenseEssentialsUi, ExpenseField>(
    componentContext = componentContext,
    id = expenseId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
    onSuccessInitData = onSuccessInitData,
    mapperToDTO = { toDto() }
) {
    private val dialogNavigation = SlotNavigation<UpdateDateTimeDialogConfig>()

    // Slot для диалога выбора даты/времени
    val dialog = childSlot(
        source = dialogNavigation,
        key = "date_time_picker_dialog",
        serializer = UpdateDateTimeDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { _, context ->
            createDateTimeDialog(context)
        }
    )

    override val columns: ImmutableList<ColumnSpec<ExpenseEssentialsUi, ExpenseField, Unit>> =
        createExpenseColumns(
            onOpenDateTimeDialog = {
                dialogNavigation.activate(UpdateDateTimeDialogConfig)
            },
            onChangeItem = ::onChangeItem
        )

    /**
     * Создаёт компонент диалога выбора даты/времени
     */
    private fun createDateTimeDialog(
        context: ComponentContext
    ): DateTimeComponent {
        val item = item.value

        return DateTimeComponent(
            componentContext = context,
            initDatetime = item.expenseDateTime,
            onDismissRequest = { dialogNavigation.dismiss() },
            onChangeDate = { newDateTime ->
                onChangeItem { it.copy(expenseDateTime = newDateTime) }
            }
        )
    }

    override val errorMessages: Flow<List<String>> = errorTableMessages

    /**
     * Конфигурация для диалога выбора даты/времени
     */
    @Serializable
    data object UpdateDateTimeDialogConfig
}

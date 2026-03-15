package ru.pavlig43.expense.internal.create.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.datetime.single.datetime.DateTimeComponent
import ru.pavlig43.expense.internal.ExpenseField
import ru.pavlig43.expense.internal.model.ExpenseEssentialsUi
import ru.pavlig43.expense.internal.model.toDto
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ua.wwind.table.ColumnSpec

/**
 * Компонент для создания расхода через таблицу с одной строкой.
 *
 * Использует [CreateSingleLineComponent] как базу и добавляет:
 * - Диалог выбора даты/времени через [SlotNavigation]
 * - Колонки таблицы для полей расхода
 * - Валидацию обязательных полей
 *
 * @param componentContext Decompose контекст компонента
 * @param onSuccessCreate Callback при успешном создании (принимает ID нового расхода)
 * @param createExpenseRepository Репозиторий для создания расхода
 */
internal class CreateExpenseSingleLineComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    observeOnItem: (ExpenseEssentialsUi) -> Unit,
    componentFactory: SingleLineComponentFactory<ExpenseBD, ExpenseEssentialsUi>,
    createExpenseRepository: CreateSingleItemRepository<ExpenseBD>,
) : CreateSingleLineComponent<ExpenseBD, ExpenseEssentialsUi, ExpenseField>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createExpenseRepository,
    mapperToDTO = ExpenseEssentialsUi::toDto,
    observeOnItem = observeOnItem
) {
    // Навигация для диалогов
    private val dialogNavigation = SlotNavigation<CreateDateTimeDialogConfig>()

    // Slot для диалога выбора даты/времени
    val dialog = childSlot(
        source = dialogNavigation,
        key = "date_time_picker_dialog",
        serializer = CreateDateTimeDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { _, context ->
            createDateTimeDialog(context)
        }
    )

    override val columns: ImmutableList<ColumnSpec<ExpenseEssentialsUi, ExpenseField, Unit>> =
        createExpenseColumns(
            onOpenDateTimeDialog = {
                dialogNavigation.activate(CreateDateTimeDialogConfig)
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

    /**
     * Конфигурация для диалога выбора даты/времени
     */
    @Serializable
    data object CreateDateTimeDialogConfig
}

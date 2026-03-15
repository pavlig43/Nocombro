package ru.pavlig43.expense.internal.create.component

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.expense.internal.ExpenseField
import ru.pavlig43.expense.internal.model.ExpenseEssentialsUi
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.writeDateTimeColumn
import ru.pavlig43.mutable.api.column.writeItemTypeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

/**
 * Создаёт колонки для таблицы создания расхода
 *
 * @param onOpenDateTimeDialog Callback для открытия диалога выбора даты/времени
 * @param onChangeItem Callback для обновления данных расхода
 */
@Suppress("LongMethod")
internal fun createExpenseColumns(
    onOpenDateTimeDialog: () -> Unit,
    onChangeItem: ((ExpenseEssentialsUi) -> ExpenseEssentialsUi) -> Unit
): ImmutableList<ColumnSpec<ExpenseEssentialsUi, ExpenseField, Unit>> {
    val columns =
        editableTableColumns<ExpenseEssentialsUi, ExpenseField, Unit> {

            // Тип расхода
            writeItemTypeColumn(
                headerText = "Тип расхода",
                column = ExpenseField.EXPENSE_TYPE,
                valueOf = { it.expenseType },
                options = ExpenseType.entries,
                isSortable = false,
                onTypeSelected = { item, type ->
                    onChangeItem { it.copy(expenseType = type) }
                }
            )

            // Сумма (в копейках)
            decimalColumn(
                key = ExpenseField.AMOUNT,
                getValue = { it.amount },
                headerText = "Сумма (₽)",
                isSortable = false,
                updateItem = { item, amount ->
                    onChangeItem { it.copy(amount = amount) }
                }
            )

            // Дата и время
            writeDateTimeColumn(
                headerText = "Дата и время",
                column = ExpenseField.EXPENSE_DATE_TIME,
                valueOf = { it.expenseDateTime },
                isSortable = false,
                onOpenDateTimeDialog = { onOpenDateTimeDialog() }
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = ExpenseField.COMMENT,
                valueOf = { it.comment },
                isSortable = false,
                onChangeItem = { item, newValue ->
                    onChangeItem { it.copy(comment = newValue) }
                }
            )
        }

    return columns
}

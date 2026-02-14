@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.expenses

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.column.writeItemTypeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongMethod")
internal fun createExpensesColumns(
    onEvent: (MutableUiEvent) -> Unit
): ImmutableList<ColumnSpec<ExpensesUi, ExpensesField, TableData<ExpensesUi>>> {
    val columns =
        editableTableColumns<ExpensesUi, ExpensesField, TableData<ExpensesUi>> {

            idWithSelection(
                selectionKey = ExpensesField.SELECTION,
                idKey = ExpensesField.COMPOSE_ID,
                onEvent = onEvent
            )
            writeItemTypeColumn(
                headerText = "Тип расхода",
                column = ExpensesField.EXPENSE_TYPE,
                valueOf = { it.expenseType },
                options = ExpenseType.entries,
                onTypeSelected = { item, type ->
                    onEvent(
                        MutableUiEvent.UpdateItem(
                            item.copy(
                                expenseType = type
                            )
                        )
                    )
                },
                filterType = TableFilterType.EnumTableFilter(
                    options = ExpenseType.entries.toImmutableList(),
                    getTitle = { it.displayName }
                ),
            )

            // Сумма в рублях
            decimalColumn(
                key = ExpensesField.AMOUNT,
                getValue = { it.amount },
                headerText = "Сумма (₽)",
                decimalFormat = DecimalFormat.RUB(),
                updateItem = { item, amount -> onEvent(MutableUiEvent.UpdateItem(item.copy(amount = amount))) },
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.amount } }
            )

            // Комментарий
            writeTextColumn(
                headerText = "Комментарий",
                column = ExpensesField.COMMENT,
                valueOf = { it.comment },
                onChangeItem = { item,newValue ->
                    onEvent(
                        MutableUiEvent.UpdateItem(
                            item.copy(comment = newValue)
                        )
                    )
                },
                filterType = TableFilterType.TextTableFilter(),
            )
        }

    return columns
}

@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.expense.internal.component.tabs.table


import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.expense.api.model.ExpenseStandaloneUi
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.column.writeItemTypeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

enum class ExpenseStandaloneField {
    SELECTION,
    COMPOSE_ID,
    EXPENSE_TYPE,
    AMOUNT,
    COMMENT
}
internal fun createExpenseStandaloneColumns(
    onEvent: (MutableUiEvent) -> Unit
): ImmutableList<ColumnSpec<ExpenseStandaloneUi, ExpenseStandaloneField, TableData<ExpenseStandaloneUi>>> {
    val columns = editableTableColumns<ExpenseStandaloneUi, ExpenseStandaloneField, TableData<ExpenseStandaloneUi>> {
        idWithSelection(
            selectionKey = ExpenseStandaloneField.SELECTION,
            idKey = ExpenseStandaloneField.COMPOSE_ID,
            onEvent = onEvent
        )

        writeItemTypeColumn(
            headerText = "Тип расхода",
            column = ExpenseStandaloneField.EXPENSE_TYPE,
            valueOf = { it.expenseType },
            options = ExpenseType.entries,
            onTypeSelected = { item, type ->
                onEvent(
                    MutableUiEvent.UpdateItem(
                        item.copy(expenseType = type)
                    )
                )
            },
            filterType = TableFilterType.EnumTableFilter(
                options = ExpenseType.entries.toImmutableList(),
                getTitle = { it.displayName }
            ),
        )

        decimalColumn(
            key = ExpenseStandaloneField.AMOUNT,
            getValue = { it.amount },
            headerText = "Сумма (₽)",
            updateItem = { item, amount ->
                onEvent(MutableUiEvent.UpdateItem(item.copy(amount = amount)))
            },
            footerValue = { tableData ->
                tableData.displayedItems.fold(DecimalData(0, DecimalFormat.Decimal2)) { acc, item ->
                    acc + item.amount
                }
            }
        )


        writeTextColumn(
            headerText = "Комментарий",
            column = ExpenseStandaloneField.COMMENT,
            valueOf = { it.comment },
            onChangeItem = { item, newValue ->
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

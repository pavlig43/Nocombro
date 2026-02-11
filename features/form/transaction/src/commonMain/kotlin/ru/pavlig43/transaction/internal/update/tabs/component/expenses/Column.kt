@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.expenses

import androidx.compose.material3.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.coreFieldBlock.ReadWriteItemTypeField
import ru.pavlig43.database.data.transaction.expense.ExpenseType
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableCellTextField
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

            // Тип расхода - dropdown
            column(ExpensesField.EXPENSE_TYPE, valueOf = { it.expenseType }) {
                header("Тип расхода")
                filter(
                    TableFilterType.EnumTableFilter(
                    options = ExpenseType.entries.toImmutableList(),
                    getTitle = { it.displayName }
                ))
                cell { item, _ ->
                    ReadWriteItemTypeField(
                        readOnly = false,
                        currentType = item.expenseType,
                        typeVariants = ExpenseType.entries,
                        onChangeType = { type ->
                            onEvent(
                                MutableUiEvent.UpdateItem(
                                    item.copy(
                                        expenseType = type
                                    )
                                )
                            )
                        }
                    )
                }
                sortable()
            }

            // Сумма в рублях
            decimalColumn(
                key = ExpensesField.AMOUNT,
                getValue = { it.amount },
                headerText = "Сумма (₽)",
                decimalFormat = DecimalFormat.RUB(),
                updateItem  = { item, amount -> onEvent(MutableUiEvent.UpdateItem(item.copy(amount = amount))) },
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.amount } }
            )

            // Комментарий
            column(ExpensesField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                filter(TableFilterType.TextTableFilter())
                cell { item, _ ->
                    TableCellTextField(
                        value = item.comment,
                        onValueChange = { newValue ->
                            onEvent(
                                MutableUiEvent.UpdateItem(
                                    item.copy(comment = newValue)
                                )
                            )
                        },
                        placeholder = { Text("Комментарий") }
                    )
                }
                sortable()
            }
        }

    return columns
}

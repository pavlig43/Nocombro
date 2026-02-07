@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.transaction.internal.component.tabs.component.expenses

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.transaction.expense.ExpenseType
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.mutable.api.ui.DecimalFormat
import ru.pavlig43.mutable.api.ui.decimalColumn
import ru.pavlig43.mutable.api.ui.idWithSelection
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
            column(ExpensesField.EXPENSE_TYPE, valueOf = { it.expenseType.displayName }) {
                header("Тип расхода")
                filter(TableFilterType.TextTableFilter())
                cell { item, _ ->
                    var expanded by remember { mutableStateOf(false) }
                    var selectedType by remember(item) { mutableStateOf(item.expenseType) }

                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        TableCellTextField(
                            value = selectedType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .clickable { expanded = true }
                        )

                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.menuAnchor()
                        ) {
                            ExpenseType.entries.forEach { type ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        selectedType = type.enumValue
                                        expanded = false
                                        onEvent(
                                            MutableUiEvent.UpdateItem(
                                                item.copy(expenseType = type.enumValue)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                sortable()
            }

            // Сумма в рублях
            decimalColumn(
                key = ExpensesField.AMOUNT,
                getValue = { it.amount },
                headerText = "Сумма (₽)",
                decimalFormat = DecimalFormat.RUB(),
                onEvent = { updateEvent -> onEvent(updateEvent) },
                updateItem = { item, newAmount -> item.copy(amount = newAmount) },
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

package ru.pavlig43.immutable.internal.component.items.transaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.ui.idWithSelection
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class TransactionField {

    SELECTION,


    ID,
    IS_COMPLETED,
    TRANSACTION_TYPE,
    CREATED_AT,
    COMMENT
}
@Suppress("LongMethod")
internal fun createTransactionColumn(
    listTypeForFilter: List<TransactionType>,
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<TransactionTableUi, TransactionField, TableData<TransactionTableUi>>> {
    val columns =
        tableColumns<TransactionTableUi, TransactionField, TableData<TransactionTableUi>> {


            idWithSelection(
                selectionKey = TransactionField.SELECTION,
                idKey = TransactionField.ID,
                onEvent = onEvent
            )
            column(TransactionField.IS_COMPLETED, valueOf = { it.isCompleted }) {
                header("V")
                align(Alignment.Center)
                cell { transaction, _ -> Icon(
                    imageVector = if (transaction.isCompleted) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (transaction.isCompleted) Color.Green else Color.Red


                ) }
                autoWidth(max = 500.dp)

            }


            column(TransactionField.TRANSACTION_TYPE, valueOf = { it.transactionType }) {
                header("Тип")
                align(Alignment.Center)
                filter(
                    TableFilterType.EnumTableFilter(
                        listTypeForFilter.toImmutableList(),
                        getTitle = { it.displayName })
                )
                cell { transaction, _ -> Text(transaction.transactionType.displayName) }
            }
            column(TransactionField.CREATED_AT, valueOf = { it.createdAt }) {
                header("Создан")
                align(Alignment.Center)
                filter(TableFilterType.DateTableFilter())
                cell { transaction, _ ->
                    Text(
                        transaction.createdAt.format(dateTimeFormat)
                    )
                }
                sortable()
            }
            column(TransactionField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { transaction, _ -> Text(transaction.comment) }
            }
        }
    return columns

}
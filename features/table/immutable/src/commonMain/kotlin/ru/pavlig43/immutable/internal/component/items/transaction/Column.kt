@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.transaction

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readDateTimeColumn
import ru.pavlig43.immutable.internal.column.readEnumColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.check
import ru.pavlig43.theme.close
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

            // Custom column for isCompleted with icon
            column(TransactionField.IS_COMPLETED, valueOf = { it.isCompleted }) {
                header("V")
                align(Alignment.Center)
                cell { transaction, _ ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(if (transaction.isCompleted) Res.drawable.check else Res.drawable.close),
                            contentDescription = null,
                            tint = if (transaction.isCompleted) Color.Green else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                autoWidth(max = 500.dp)
            }

            readEnumColumn(
                headerText = "Тип",
                column = TransactionField.TRANSACTION_TYPE,
                valueOf = { it.transactionType },
                filterType = TableFilterType.EnumTableFilter(
                    listTypeForFilter.toImmutableList(),
                    getTitle = { it.displayName }
                ),
                getTitle = { it.displayName }
            )

            readDateTimeColumn(
                headerText = "Создан",
                column = TransactionField.CREATED_AT,
                valueOf = { it.createdAt },
                filterType = TableFilterType.DateTableFilter()
            )

            readTextColumn(
                headerText = "Комментарий",
                column = TransactionField.COMMENT,
                valueOf = { it.comment },
                filterType = TableFilterType.TextTableFilter()
            )
        }
    return columns

}

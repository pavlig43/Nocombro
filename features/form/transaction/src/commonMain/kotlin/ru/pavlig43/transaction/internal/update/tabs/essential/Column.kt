@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.transaction.internal.update.tabs.essential

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.readItemTypeColumn
import ru.pavlig43.mutable.api.column.writeCheckBoxColumn
import ru.pavlig43.mutable.api.column.writeDateTimeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.transaction.internal.TransactionField
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

internal fun createTransactionColumns1(
    onOpenCreatedAtDialog: () -> Unit,
    onChangeItem: (TransactionEssentialsUi) -> Unit,
): ImmutableList<ColumnSpec<TransactionEssentialsUi, TransactionField, Unit>> {
    val columns =
        editableTableColumns<TransactionEssentialsUi, TransactionField, Unit> {

            readItemTypeColumn(
                headerText = "Тип транзакции",
                column = TransactionField.TRANSACTION_TYPE,
                valueOf = { it.transactionType },
            )

            writeDateTimeColumn(
                headerText = "Дата/время",
                column = TransactionField.CREATED_AT,
                valueOf = { it.createdAt },
                onOpenDateTimeDialog = {onOpenCreatedAtDialog()},
            )

            writeCheckBoxColumn(
                headerText = "Проведена",
                column = TransactionField.IS_COMPLETED,
                valueOf = { it.isCompleted },
                onChangeChecked = { item, checked -> onChangeItem(item.copy(isCompleted = checked)) },
            )

            writeTextColumn(
                headerText = "Комментарий",
                column = TransactionField.COMMENT,
                valueOf = { it.comment },
                onChangeItem = { item, comment -> onChangeItem(item.copy(comment = comment)) },
                singleLine = false
            )
        }
    return columns
}

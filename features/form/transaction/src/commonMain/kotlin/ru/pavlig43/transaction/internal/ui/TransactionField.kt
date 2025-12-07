package ru.pavlig43.transaction.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.coreui.coreFieldBlock.CommentFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.DateFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.ItemTypeField
import ru.pavlig43.coreui.coreFieldBlock.LabelCheckBoxFieldBlock
import ru.pavlig43.database.data.transaction.OperationType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.transaction.internal.data.TransactionEssentialsUi

@Composable
internal fun TransactionFields(
    transaction: TransactionEssentialsUi,
    updateTransaction: (TransactionEssentialsUi) -> Unit,
) {

    DateFieldBlock(
        date = transaction.createdAt,
        onSelectDate = { updateTransaction(transaction.copy(createdAt = it)) },
        dateName = "Дата время проведения",
        convertDateToUi = {this.convertToDateTime()}
    )
    ItemTypeField(
        typeVariants = OperationType.entries,
        currentType = transaction.operationType,
        onChangeType = { updateTransaction(transaction.copy(operationType = it)) }
    )
    ItemTypeField(
        typeVariants = TransactionType.entries,
        currentType = transaction.transactionType,
        onChangeType = { updateTransaction(transaction.copy(transactionType = it)) }
    )
    LabelCheckBoxFieldBlock(
        checked = transaction.isCompleted,
        onChangeChecked = { updateTransaction(transaction.copy(isCompleted = it)) },
        label = "Проведена и отображается в отчетах",
    )

    CommentFieldBlock(
        comment = transaction.comment,
        onChangeComment = { updateTransaction(transaction.copy(comment = it)) }
    )
}
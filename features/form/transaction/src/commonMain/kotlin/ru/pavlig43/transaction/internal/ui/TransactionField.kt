package ru.pavlig43.transaction.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.coreui.coreFieldBlock.CommentFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.DateTimeFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.LabelCheckBoxFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.ReadWriteItemTypeField
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi

@Composable
internal fun TransactionFields(
    transaction: TransactionEssentialsUi,
    updateTransaction: (TransactionEssentialsUi) -> Unit,
) {

    DateTimeFieldBlock(
        dateTime = transaction.createdAt,
        onSelectDateTime = { updateTransaction(transaction.copy(createdAt = it)) },
        dateName = "Дата время проведения",
    )
    ReadWriteItemTypeField(
        readOnly = transaction.id != 0,
        currentType = transaction.transactionType,
        typeVariants = TransactionType.entries,
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
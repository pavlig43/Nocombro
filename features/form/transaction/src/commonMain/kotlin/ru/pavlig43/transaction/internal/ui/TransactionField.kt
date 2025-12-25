package ru.pavlig43.transaction.internal.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.coreFieldBlock.CommentFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.DateTimeFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.ItemTypeField
import ru.pavlig43.coreui.coreFieldBlock.LabelCheckBoxFieldBlock
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
    if (transaction.id == 0) {
        ItemTypeField(
            typeVariants = TransactionType.entries,
            currentType = transaction.transactionType,
            onChangeType = { updateTransaction(transaction.copy(transactionType = it)) }
        )
    } else {
        Card {
            Text(transaction.transactionType?.displayName ?: "*", Modifier.padding(4.dp))
        }

    }

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
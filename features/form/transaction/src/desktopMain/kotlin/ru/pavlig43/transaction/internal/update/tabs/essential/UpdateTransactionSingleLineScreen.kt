package ru.pavlig43.transaction.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.transaction.internal.TransactionEssentialsCards

/** Показывает карточную форму правки транзакции. */
@Composable
internal fun UpdateTransactionSingleLineScreen(
    component: TransactionUpdateSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    TransactionEssentialsCards(
        component = component,
        onOpenCreatedAtDialog = component::onOpenCreatedAtDialog,
    )

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is UpdateDialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}

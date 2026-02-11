package ru.pavlig43.transaction.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineBlockScreen
import ru.pavlig43.transaction.internal.update.tabs.essential.TransactionUpdateSingleLineComponent
import ru.pavlig43.transaction.internal.update.tabs.essential.UpdateDialogChild

@Composable
internal fun UpdateTransactionSingleLineScreen(
    component: TransactionUpdateSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    SingleLineBlockScreen(component)

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is UpdateDialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}

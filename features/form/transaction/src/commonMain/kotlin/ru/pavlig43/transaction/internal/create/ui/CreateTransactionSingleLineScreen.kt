package ru.pavlig43.transaction.internal.create.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen
import ru.pavlig43.transaction.internal.create.component.CreateTransactionSingleLineComponent
import ru.pavlig43.transaction.internal.create.component.DialogChild

@Composable
internal fun CreateTransactionSingleLineScreen(
    component: CreateTransactionSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    CreateSingleItemScreen(component)

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}

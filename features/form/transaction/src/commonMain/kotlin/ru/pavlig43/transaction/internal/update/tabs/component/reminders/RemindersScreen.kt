package ru.pavlig43.transaction.internal.update.tabs.component.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox

@Composable
internal fun RemindersScreen(
    component: RemindersComponent
) {
    val dialog by component.dialog.subscribeAsState()

    MutableTableBox(component)

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}

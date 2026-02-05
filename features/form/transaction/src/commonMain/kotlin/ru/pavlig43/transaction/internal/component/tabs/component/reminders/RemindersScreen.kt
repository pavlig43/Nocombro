package ru.pavlig43.transaction.internal.component.tabs.component.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.mutable.api.ui.MutableTableBox

@Composable
internal fun RemindersScreen(
    component: RemindersComponent
) {
    val dialog by component.dialog.subscribeAsState()

    MutableTableBox(
        component,
        tableSettingsModify = { ts -> ts.copy(showFooter = false) }
    )

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}

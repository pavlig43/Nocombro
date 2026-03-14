package ru.pavlig43.expense.internal.component.tabs.table

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.period.dateTime.DateTimeRow
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineBlockScreen

@Composable
internal fun TableScreen(component: TableComponent) {
    val dialog by component.dialog.subscribeAsState()

    SingleLineBlockScreen(component)

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}

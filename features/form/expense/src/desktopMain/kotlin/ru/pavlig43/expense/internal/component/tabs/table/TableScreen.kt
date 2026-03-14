package ru.pavlig43.expense.internal.component.tabs.table

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.period.dateTime.DateTimeRow
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox

@Composable
internal fun TableScreen(component: TableComponent) {
    val dialog by component.dialog.subscribeAsState()
    val dateTime by component.datetime.collectAsState()
    Column {
        DateTimeRow(
            label = "Время",
            dateTime = dateTime,
            onClick = component::openDateTimeDialog,
        )
        MutableTableBox(component)
    }

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> {
                DateTimePickerDialog(dialogChild.component)
            }
        }
    }
}

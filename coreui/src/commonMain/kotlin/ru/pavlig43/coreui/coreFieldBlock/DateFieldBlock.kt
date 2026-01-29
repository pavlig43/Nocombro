package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.coreui.DateRow
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.calendar
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun DateFieldBlock(
    date: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    dateName: String,
    modifier: Modifier = Modifier
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateName)
        DateRow(
            date = date,
            isChangeDialogVisible = {isDatePickerVisible = !isDatePickerVisible}
        )

        ToolTipIconButton(
            tooltipText = "Дата",
            onClick = { isDatePickerVisible = !isDatePickerVisible },
            icon = Res.drawable.calendar

        )
        if (isDatePickerVisible){
            DatePickerDialog(
                onDismissRequest = {isDatePickerVisible = false},
                date = date,
                onSelectDate = onSelectDate
            )
        }


    }
}

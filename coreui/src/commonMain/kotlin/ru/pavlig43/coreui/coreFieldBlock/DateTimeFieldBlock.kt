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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.coreui.DatePickerDialog
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun DateTimeFieldBlock(
    dateTime: LocalDateTime,
    onSelectDateTime: (LocalDateTime) -> Unit,
    dateName: String,
    modifier: Modifier = Modifier
) {

    var isDateTimePickerVisible by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateName)
        DateTimeRow(
            date = dateTime,
            isChangeDialogVisible = { isDateTimePickerVisible = !isDateTimePickerVisible }
        )

        ToolTipIconButton(
            tooltipText = "Дата",
            onClick = { isDateTimePickerVisible = !isDateTimePickerVisible },
            icon = Res.drawable.clock

        )
        if (isDateTimePickerVisible) {
            DateTimePickerDialog(
                onDismissRequest = { isDateTimePickerVisible = false },
                dateTime = dateTime,
                onSelectDateTime = onSelectDateTime,
            )
        }
    }

}
@Composable
fun DateTimeRow(
    date: LocalDateTime,
    isChangeDialogVisible: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(date.format(dateTimeFormat), textDecoration = TextDecoration.Underline)

        ToolTipIconButton(
            tooltipText = "Дата/время",
            onClick = isChangeDialogVisible,
            icon = Res.drawable.clock

        )
    }
}
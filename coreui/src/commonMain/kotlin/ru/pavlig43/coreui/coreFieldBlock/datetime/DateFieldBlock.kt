package ru.pavlig43.coreui.coreFieldBlock.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
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
import ru.pavlig43.core.DateFieldKind
import ru.pavlig43.core.convertToDateOrDateTimeString
import ru.pavlig43.core.getUTCNow
import ru.pavlig43.coreui.coreFieldBlock.datetime.internal.DateOrDateTimeDialog
import ru.pavlig43.coreui.coreFieldBlock.datetime.internal.DatePicker
import ru.pavlig43.coreui.coreFieldBlock.datetime.internal.DateTimePicker
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun DateFieldBlock(
    dateTime: Long?,
    onSelectDate: (Long) -> Unit,
    dateFieldKind: DateFieldKind,
    dateName: String,
    modifier: Modifier = Modifier
) {

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateName)
        Text(dateTime?.convertToDateOrDateTimeString(dateFieldKind)?:"", textDecoration = TextDecoration.Underline)
        var isDatePickerVisible by remember { mutableStateOf(false) }
        IconButtonToolTip(
            tooltipText = when (dateFieldKind) {
                DateFieldKind.Date -> "Дата"
                DateFieldKind.DateTime -> "Дата/Время"
            },
            onClick = { isDatePickerVisible = true },
            icon = when (dateFieldKind) {
                DateFieldKind.Date -> Icons.Default.CalendarMonth
                DateFieldKind.DateTime -> Icons.Default.AccessTime
            },
        )

        if (isDatePickerVisible) {
            DateOrDateTimeDialog(
                initialDateTime = dateTime ?: getUTCNow(),
                onConfirm = onSelectDate,
                onDismissRequest = { isDatePickerVisible = false },
                dateTimePicker = { dateTime1, timeZone, onChange ->
                    when (dateFieldKind) {
                        DateFieldKind.Date -> DatePicker(dateTime1, timeZone, onChange)
                        DateFieldKind.DateTime -> DateTimePicker(dateTime1, timeZone, onChange)
                    }

                }
            )

        }

    }
}


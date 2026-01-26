package ru.pavlig43.coreui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.coreui.tooltip.ToolTipIconButton

@Composable
fun DateTimePicker(
    dateTime: LocalDateTime,
    onSelectDateTime: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(dateTime.format(dateTimeFormat), textDecoration = TextDecoration.Underline)
        var isDatePickerVisible by remember { mutableStateOf(false) }
        ToolTipIconButton(
            tooltipText = "Дата/Время",
            onClick = { isDatePickerVisible = !isDatePickerVisible },
            icon = Icons.Default.CalendarMonth

        )

        if (isDatePickerVisible) {
            ProjectDialog(
                onDismissRequest = {isDatePickerVisible = false},
                header = {
                    Text(
                        text = "Выберите дату/время",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            ) {
                WheelDateTimePicker(
                    startDateTime = dateTime,
                    dateFormatter = dateFormatter(
                        locale = Locale.current,
                        monthDisplayStyle = MonthDisplayStyle.SHORT,
                        cjkSuffixConfig = CjkSuffixConfig.HideAll
                    ),

                    rowCount = 5,
                    textStyle = MaterialTheme.typography.titleSmall,
                    textColor = MaterialTheme.colorScheme.primary,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                    )
                ) { snappedDateTime ->
                    onSelectDateTime(snappedDateTime)
                }
            }


        }

    }
}

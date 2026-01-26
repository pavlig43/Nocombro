package ru.pavlig43.coreui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.coreui.tooltip.ToolTipIconButton

@Composable
fun DatePicker(
    date: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(date.format(dateFormat), textDecoration = TextDecoration.Underline)
        var isDatePickerVisible by remember { mutableStateOf(false) }
        ToolTipIconButton(
            tooltipText = "Дата",
            onClick = { isDatePickerVisible = !isDatePickerVisible },
            icon = Icons.Default.AccessTime

        )

        if (isDatePickerVisible) {
            ProjectDialog(
                onDismissRequest = {isDatePickerVisible = false},
                header = {
                    Text(
                        text = "Выберите дату",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            ) {

                WheelDatePicker(
                    startDate = date,
                    dateFormatter = dateFormatter(
                        locale = Locale.current,
                        monthDisplayStyle = MonthDisplayStyle.SHORT,
                        cjkSuffixConfig = CjkSuffixConfig.HideAll
                    ),
                    rowCount = 5,
                    textStyle = MaterialTheme.typography.titleSmall,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                    )
                ) { snappedDateTime: LocalDate ->
                    onSelectDate(snappedDateTime)
                }

            }
        }
    }


}

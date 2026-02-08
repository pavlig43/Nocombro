package ru.pavlig43.coreui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import ru.pavlig43.core.DateComponent
import ru.pavlig43.core.dateFormat
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.calendar

@Composable
fun DatePickerDialog(
    dateComponent: DateComponent
){
    val date by dateComponent.date.collectAsState()
    DatePickerDialog(
        date = date,
        onDismissRequest = {dateComponent.onDismissRequest()},
        onSelectDate = {dateComponent.onChangeDate(it)}
    )

}
@Composable
fun DatePickerDialog(
    date: LocalDate,
    onDismissRequest:()-> Unit,
    onSelectDate: (LocalDate) -> Unit
) {

    ProjectDialog(
        onDismissRequest = onDismissRequest,
        header = {
            Text(
                text = "Выберите дату",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
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
@Composable
fun DateRow(
    date: LocalDate,
    isChangeDialogVisible:()-> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolTipIconButton(
            tooltipText = "Дата",
            onClick = isChangeDialogVisible,
            icon = Res.drawable.calendar

        )

        Text(date.format(dateFormat), textDecoration = TextDecoration.Underline)
}
}


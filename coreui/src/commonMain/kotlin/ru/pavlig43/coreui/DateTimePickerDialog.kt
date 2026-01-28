package ru.pavlig43.coreui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.DateTimeComponent


@Composable
fun DateTimePickerDialog(
    dateTimeComponent: DateTimeComponent
){
    val dateTime by dateTimeComponent.dateTime.collectAsState()
    DateTimePickerDialog(
        dateTime = dateTime,
        onDismissRequest = { dateTimeComponent.onDismissRequest() },
        onSelectDateTime = { dateTimeComponent.onChangeDate(it) },
    )

}
@Composable
fun DateTimePickerDialog(
    dateTime: LocalDateTime,
    onDismissRequest:()-> Unit,
    onSelectDateTime: (LocalDateTime) -> Unit
) {

    ProjectDialog(
        onDismissRequest = onDismissRequest,
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

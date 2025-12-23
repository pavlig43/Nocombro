package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import ru.pavlig43.core.dateFormat
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun DateFieldBlock(
    date: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    dateName: String,
    modifier: Modifier = Modifier
) {

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateName)
        Text(date.format(dateFormat), textDecoration = TextDecoration.Underline)
        var isDatePickerVisible by remember { mutableStateOf(false) }
        IconButtonToolTip(
            tooltipText = "Дата",
            onClick = { isDatePickerVisible = true },
            icon = Icons.Default.AccessTime

        )

        if (isDatePickerVisible) {
            WheelDatePicker(
                startDate = date,
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
            ) { snappedDateTime: LocalDate ->
                onSelectDate(snappedDateTime)
            }

        }

    }
}

package ru.pavlig43.coreui.coreFieldBlock.datetime.internal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.TimeFormat
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import dev.darkokoa.datetimewheelpicker.core.format.timeFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
internal fun DateTimePicker(
    dateTime: Long,
    timeZone: TimeZone,
    onDateTimeChange: (Long) -> Unit
) {
    WheelDateTimePicker(
        startDateTime = Instant.fromEpochMilliseconds(dateTime)
            .toLocalDateTime(timeZone),
        dateFormatter = dateFormatter(
            locale = Locale.current,
            monthDisplayStyle = MonthDisplayStyle.SHORT,
            cjkSuffixConfig = CjkSuffixConfig.HideAll
        ),
        timeFormatter = timeFormatter(
            timeFormat = TimeFormat.HOUR_24
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
        onDateTimeChange(snappedDateTime.toInstant(timeZone).toEpochMilliseconds())
    }
}
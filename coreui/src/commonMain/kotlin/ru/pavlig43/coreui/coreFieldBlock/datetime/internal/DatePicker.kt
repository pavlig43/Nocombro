package ru.pavlig43.coreui.coreFieldBlock.datetime.internal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
internal fun DatePicker(
    date: Long,
    timeZone: TimeZone,
    onDateChange: (Long) -> Unit
) {
    WheelDatePicker(
        startDate = Instant.Companion.fromEpochMilliseconds(date)
            .toLocalDateTime(timeZone).date,
        dateFormatter = dateFormatter(
            locale = Locale.Companion.current,
            monthDisplayStyle = MonthDisplayStyle.SHORT,
            cjkSuffixConfig = CjkSuffixConfig.Companion.HideAll
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
        onDateChange(
            LocalDateTime(snappedDateTime, LocalTime(0, 0))
                .toInstant(timeZone)
                .toEpochMilliseconds()
        )
    }
}
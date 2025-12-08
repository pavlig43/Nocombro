package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.TimeFormat
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import dev.darkokoa.datetimewheelpicker.core.format.timeFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
@Composable
fun DateTimePicker() {
    var date by remember {
        mutableStateOf(
            LocalDateTime(
                year = 2025,
                month = 10,
                day = 20,
                hour = 5,
                minute = 30
            )
        )
    }
    LaunchedEffect(date) {
        println(date.format(
            format = LocalDateTime.Format {
                dayOfMonth()
                char('-')
                monthNumber()
                char('-')
                year()


                char('-')

                char(' ')
                hour()
                char(':')
                minute()
            }
        ))
    }

    WheelDateTimePicker(
        startDateTime = date,
//        minDateTime = Clock.System
//            .now()
//            .toLocalDateTime(TimeZone.currentSystemDefault()),
//        maxDateTime = LocalDateTime(
//            year = 2027,
//            month = 10,
//            day = 20,
//            hour = 5,
//            minute = 30
//        ),
        dateFormatter = dateFormatter(
            locale = Locale.current,
            monthDisplayStyle = MonthDisplayStyle.SHORT,
            cjkSuffixConfig = CjkSuffixConfig.HideAll
        ),
        timeFormatter = timeFormatter(
            timeFormat = TimeFormat.HOUR_24
        ),
        size = DpSize(200.dp, 100.dp),
        rowCount = 5,
        textStyle = MaterialTheme.typography.titleSmall,
        textColor = Color(0xFFffc300),
        selectorProperties = WheelPickerDefaults.selectorProperties(
            enabled = true,
            shape = RoundedCornerShape(0.dp),
            color = Color(0xFFf1faee).copy(alpha = 0.2f),
            border = BorderStroke(2.dp, Color(0xFFf1faee))
        )
    ) { snappedDateTime: LocalDateTime ->
        date = snappedDateTime
    }
}
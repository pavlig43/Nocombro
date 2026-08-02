package ru.pavlig43.datetime.period.dateTime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.clock

@Composable
fun DateTimeSelectorScreen(
    component: DateTimePeriodComponent
){
    val internalDateTimePeriod by component.dateTimePeriod.collectAsState()
    val dateTimePeriodForData by component.dateTimePeriodForData.collectAsState()
    val dialog by component.dialog.subscribeAsState()
    DateTimeSelectorScreen(
        startDateTime = internalDateTimePeriod.start,
        endDateTime = internalDateTimePeriod.end,
        onStartClick = component::openStartDateTimeDialog,
        onEndClick = component::openEndDateTimeDialog,
        updateDateTimePeriod = component::updateDateTimePeriod,
    )
    if (internalDateTimePeriod != dateTimePeriodForData) {
        Text(
            text = "Данные за $dateTimePeriodForData",
            modifier = Modifier.padding(start = 24.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DateTimeDialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}
@Suppress("LongParameterList")
@Composable
private fun DateTimeSelectorScreen(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    updateDateTimePeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(start = 24.dp, top = 6.dp, bottom = 6.dp, end = 24.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateTimeRow(
                label = "С",
                dateTime = startDateTime,
                onClick = onStartClick,
            )
            Text(
                text = "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DateTimeRow(
                label = "По",
                dateTime = endDateTime,
                onClick = onEndClick,
            )
            Button(
                onClick = updateDateTimePeriod,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = startDateTime <= endDateTime,
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.clock),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Показать",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
fun DateTimeRow(
    label: String,
    dateTime: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.clock),
                contentDescription = label,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = dateTime.format(dateTimeFormat),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

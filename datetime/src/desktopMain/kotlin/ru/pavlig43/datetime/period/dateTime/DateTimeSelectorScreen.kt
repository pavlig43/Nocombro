package ru.pavlig43.datetime.period.dateTime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * Показывает выбор начала и конца периода вертикально для узкой боковой панели.
 *
 * Поля дат занимают всю доступную ширину, а строка состояния всегда резервирует
 * одинаковую высоту, чтобы соседние блоки не смещались после применения периода.
 *
 * @param component компонент, который хранит выбранный и применённый периоды.
 * @param modifier модификатор корневой вертикальной панели.
 */
@Composable
@Suppress("LongMethod")
fun VerticalDateTimeSelectorScreen(
    component: DateTimePeriodComponent,
    modifier: Modifier = Modifier,
) {
    val internalDateTimePeriod by component.dateTimePeriod.collectAsState()
    val dateTimePeriodForData by component.dateTimePeriodForData.collectAsState()
    val dialog by component.dialog.subscribeAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Период",
            style = MaterialTheme.typography.titleSmall,
        )
        DateTimeRow(
            label = "Начало периода",
            dateTime = internalDateTimePeriod.start,
            onClick = component::openStartDateTimeDialog,
            modifier = Modifier.fillMaxWidth(),
        )
        DateTimeRow(
            label = "Конец периода",
            dateTime = internalDateTimePeriod.end,
            onClick = component::openEndDateTimeDialog,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = component::updateDateTimePeriod,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(10.dp),
            enabled = internalDateTimePeriod.start <= internalDateTimePeriod.end,
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
        Box(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (internalDateTimePeriod != dateTimePeriodForData) {
                Text(
                    text = "Данные за $dateTimePeriodForData",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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

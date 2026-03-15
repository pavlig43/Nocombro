package ru.pavlig43.datetime.period.dateTime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
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
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
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
    Text(
        "Выбранный период $dateTimePeriodForData",
        Modifier.padding(start = 24.dp)
    )
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
    Card(
        modifier = modifier.padding(start = 24.dp, top = 12.dp, bottom = 12.dp, end = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Заголовок секции
            Text(
                text = "Период",
                style = MaterialTheme.typography.titleMedium,
                color = contentColorFor(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(Modifier.width(8.dp))

            // Дата начала
            DateTimeRow(
                label = "Начало",
                dateTime = startDateTime,
                onClick = onStartClick
            )

            // Разделитель
            Text(
                text = "—",
                style = MaterialTheme.typography.titleLarge,
                color = contentColorFor(MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.6f)
            )

            // Дата конца
            DateTimeRow(
                label = "Конец",
                dateTime = endDateTime,
                onClick = onEndClick
            )

            Spacer(Modifier.width(8.dp))

            // Кнопка поиска
            Button(
                onClick = updateDateTimePeriod,
                shape = RoundedCornerShape(12.dp),
                enabled = startDateTime <= endDateTime
            ) {
                Icon(
                    painter = painterResource(Res.drawable.clock),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text("Поиск")
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolTipIconButton(
            tooltipText = label,
            onClick = onClick,
            icon = Res.drawable.clock
        )
        Text(dateTime.format(dateTimeFormat))
    }
}
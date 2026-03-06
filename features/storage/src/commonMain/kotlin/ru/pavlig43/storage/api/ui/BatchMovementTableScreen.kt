package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementField
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementInfo
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementLoadState
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementComponent
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementTableData
import ru.pavlig43.storage.api.component.batchMovement.DialogChild
import ru.pavlig43.storage.api.component.batchMovement.createBatchMovementColumns
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ru.pavlig43.tablecore.ui.ScrollBar
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.clock
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState

@OptIn(ExperimentalTableApi::class)
@Composable
fun BatchMovementTableScreen(
    component: BatchMovementComponent,
    modifier: Modifier = Modifier
) {
    val dateTimePeriodUi by component.dateTimePeriodUi.collectAsState()
    val dateTimePeriodForData by component.dateTimePeriodForData.collectAsState()
    val dialog by component.dialog.subscribeAsState()

    PeriodSelectorRow(
        startDateTime = dateTimePeriodUi.start,
        endDateTime = dateTimePeriodUi.end,
        onStartClick = { component.openStartDateTimeDialog() },
        onEndClick = { component.openEndDateTimeDialog() },
        onSearchNewData = component::updateDateTimePeriod
    )

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }

    val loadState by component.loadState.collectAsState()

    when (val state = loadState) {
        is BatchMovementLoadState.Error -> ErrorScreen(state.message)
        is BatchMovementLoadState.Loading -> LoadingUi()
        is BatchMovementLoadState.Success -> {
            // Card с информацией о продукте и партии
            BatchInfoCard(
                productName = state.info.productName,
                batchName = state.info.batchName,
                startPeriod = dateTimePeriodForData.start.format(dateTimeFormat),
                endPeriod = dateTimePeriodForData.end.format(dateTimeFormat)
            )

            val tableData = remember(state.info.movements) { BatchMovementTableData(state.info.movements) }
            val columns = remember { createBatchMovementColumns() }
            val tableState = rememberTableState(
                columns = BatchMovementField.entries.toImmutableList()
            )
            val verticalState = rememberLazyListState()

            BatchMovementTable(
                state = tableState,
                tableData = tableData,
                columns = columns,
                verticalState = verticalState,
                onRowClick = component::onRowClick,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalTableApi::class)
@Composable
private fun BatchMovementTable(
    state: TableState<BatchMovementField>,
    tableData: BatchMovementTableData,
    columns: ImmutableList<ColumnSpec<ru.pavlig43.storage.api.component.batchMovement.BatchMovementTableUi, BatchMovementField, BatchMovementTableData>>,
    verticalState: androidx.compose.foundation.lazy.LazyListState,
    onRowClick: (ru.pavlig43.storage.api.component.batchMovement.BatchMovementTableUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalState = androidx.compose.foundation.rememberScrollState()

    Box(
        modifier = modifier.padding(start = 24.dp)
    ) {
        Table(
            itemsCount = tableData.items.size,
            itemAt = { index -> tableData.items.getOrNull(index) },
            state = state,
            columns = columns,
            tableData = tableData,
            onRowClick = onRowClick,
            strings = RussianStringProvider,
            verticalState = verticalState,
            horizontalState = horizontalState,
            modifier = modifier,
            colors = TableDefaults.colors(
                headerContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        )
        ScrollBar(
            verticalState = verticalState,
            horizontalState = horizontalState
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun PeriodSelectorRow(
    startDateTime: kotlinx.datetime.LocalDateTime,
    endDateTime: kotlinx.datetime.LocalDateTime,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onSearchNewData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(start = 24.dp, top = 12.dp, bottom = 12.dp, end = 24.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
            Text(
                text = "Период",
                style = MaterialTheme.typography.titleMedium,
                color = contentColorFor(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(Modifier.width(8.dp))

            DateTimeRow(
                label = "Начало",
                dateTime = startDateTime,
                onClick = onStartClick
            )

            Text(
                text = "—",
                style = MaterialTheme.typography.titleLarge,
                color = contentColorFor(MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.6f)
            )

            DateTimeRow(
                label = "Конец",
                dateTime = endDateTime,
                onClick = onEndClick
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onSearchNewData,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
private fun DateTimeRow(
    label: String,
    dateTime: kotlinx.datetime.LocalDateTime,
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

@Composable
private fun BatchInfoCard(
    productName: String,
    batchName: String,
    startPeriod: String,
    endPeriod: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp, end = 24.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Информация о партии",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text("Продукт: $productName", style = MaterialTheme.typography.bodyMedium)
            Text("Партия: $batchName", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Период: $startPeriod — $endPeriod",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

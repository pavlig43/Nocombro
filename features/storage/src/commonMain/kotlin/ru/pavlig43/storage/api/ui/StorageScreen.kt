package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.format
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.storage.api.component.LoadState
import ru.pavlig43.storage.api.component.StorageComponent
import ru.pavlig43.storage.api.component.StorageProductField
import ru.pavlig43.storage.api.component.DialogChild
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.clock
import ru.pavlig43.storage.api.component.createStorageColumns
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.state.TableState
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ua.wwind.table.Table
import ru.pavlig43.tablecore.ui.ScrollBar
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState

@Composable
fun StorageScreen(
    component: StorageComponent
){
    val dateTimePeriod by component.dateTimePeriodUi.collectAsState()
    val dialog by component.dialog.subscribeAsState()

    PeriodSelectorRow(
        startDateTime = dateTimePeriod.start,
        endDateTime = dateTimePeriod.end,
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
    when(val state = loadState) {
        is LoadState.Error -> ErrorScreen(state.message)
        is LoadState.Loading -> LoadingUi()
        is LoadState.Success -> {
            val columns = remember { createStorageColumns(component::toggleExpand) }
            val tableSettings = remember {
                TableSettings(
                    showActiveFiltersHeader = true,
                )
            }
            val tableState = rememberTableState(
                columns = StorageProductField.entries.toImmutableList(),
                settings = tableSettings,
            )
            LaunchedEffect(tableState) {
                snapshotFlow { tableState.filters.toMap() }.collect { filters -> component.updateFilters(filters) }
            }
            val tableData by component.tableData.collectAsState()

            StorageTable(
                state = tableState,
                tableData = tableData,
                columns = columns,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
@OptIn(ExperimentalTableApi::class)
@Composable
private fun StorageTable(
    state: TableState<StorageProductField>,
    tableData: StorageTableData,
    columns: ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, StorageTableData>>,
    modifier: Modifier = Modifier,
) {
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()

    val customization = remember { StorageTableCustomization() }

    Box(
        modifier = Modifier.fillMaxSize().padding(start = 24.dp),
    ) {
        Table(
            itemsCount = tableData.displayedProducts.size,
            itemAt = { index -> tableData.displayedProducts.getOrNull(index) },
            state = state,
            columns = columns,
            tableData = tableData,
            onRowClick = { },
            strings = RussianStringProvider,
            customization = customization,
            verticalState = verticalState,
            horizontalState = horizontalState,
            modifier = modifier,
        )
        ScrollBar(
            verticalState = verticalState,
            horizontalState = horizontalState,
        )

    }
}

private class StorageTableCustomization : TableCustomization<StorageProductUi, StorageProductField> {
    @Composable
    override fun resolveRowStyle(ctx: TableRowContext<StorageProductUi, StorageProductField>): TableRowStyle {
        return if (!ctx.item.isProduct) {
            TableRowStyle(
                contentColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = 2.dp,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            )
        } else TableRowStyle()
    }

    @Composable
    override fun resolveCellStyle(ctx: TableCellContext<StorageProductUi, StorageProductField>): TableCellStyle {
        val item = ctx.row.item
        val cellValue = when (ctx.column) {
            StorageProductField.BALANCE_BEFORE -> item.balanceBeforeStart
            StorageProductField.INCOMING -> item.incoming
            StorageProductField.OUTGOING -> item.outgoing
            StorageProductField.BALANCE_END -> item.balanceOnEnd
            else -> null
        }
        return if (cellValue != null && cellValue < 0) {
            TableCellStyle(
                background = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else TableCellStyle()
    }
}

@Composable
private fun PeriodSelectorRow(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onSearchNewData:()-> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Период:")

        DateTimeRow(
            label = "Начало",
            dateTime = startDateTime,
            onClick = onStartClick
        )

        Text("—")

        DateTimeRow(
            label = "Конец",
            dateTime = endDateTime,
            onClick = onEndClick
        )
        Button(onSearchNewData){
            Text("Поиск")
        }
    }
}

@Composable
private fun DateTimeRow(
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

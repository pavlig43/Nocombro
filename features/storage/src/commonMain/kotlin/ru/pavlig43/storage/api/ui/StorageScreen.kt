package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.coreui.DateTimePickerDialog
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.storage.api.component.storage.DialogChild
import ru.pavlig43.storage.api.component.storage.LoadState
import ru.pavlig43.storage.api.component.storage.StorageComponent
import ru.pavlig43.storage.api.component.storage.StorageProductField
import ru.pavlig43.storage.api.component.storage.createStorageColumns
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ru.pavlig43.tablecore.ui.ScrollBar
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.clock
import ru.pavlig43.theme.warning
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState

@Suppress("LongMethod")
@Composable
fun StorageScreen(
    component: StorageComponent
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
    Text(
        "Выбранный период ${dateTimePeriodForData.start.format(dateTimeFormat)} - ${
            dateTimePeriodForData.end.format(
                dateTimeFormat
            )
        }",
        Modifier.padding(start = 24.dp)
    )


    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }

    val loadState by component.loadState.collectAsState()
    when (val state = loadState) {
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
                snapshotFlow { tableState.filters.toMap() }.collect { filters ->
                    component.updateFilters(
                        filters
                    )
                }
            }
            val tableData by component.tableData.collectAsState()
            val verticalState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            val negativeBatches = remember(tableData) {
                getNegativeBatches(tableData).toImmutableList()
            }

            if (negativeBatches.isNotEmpty()) {
                NegativeBatchesCard(
                    negativeBatches = negativeBatches,
                    onBatchClick = { productId, itemId ->
                        coroutineScope.launch {
                            // Сначала раскрываем продукт
                            component.toggleExpand(productId)

                            // Получаем актуальные данные
                            val currentData = component.tableData.value
                            val newIndex = currentData.displayedProducts
                                .indexOfFirst { it.itemId == itemId }

                            if (newIndex >= 0) {
                                verticalState.animateScrollToItem(
                                    index = newIndex,
                                    scrollOffset = -80
                                )
                            }
                        }
                    }
                )
            }

            StorageTable(
                state = tableState,
                tableData = tableData,
                columns = columns,
                verticalState = verticalState,
                onRowClick = component::onRowClick,
            )
        }
    }
}

@OptIn(ExperimentalTableApi::class)
@Suppress("MagicNumber")
@Composable
private fun StorageTable(
    state: TableState<StorageProductField>,
    tableData: StorageTableData,
    columns: ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, StorageTableData>>,
    verticalState: androidx.compose.foundation.lazy.LazyListState,
    onRowClick: (StorageProductUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalState = rememberScrollState()

    val customization = remember { StorageTableCustomization() }

    Box(
        modifier = modifier.padding(start = 24.dp),
    ) {
        Table(
            itemsCount = tableData.displayedProducts.size,
            itemAt = { index -> tableData.displayedProducts.getOrNull(index) },
            state = state,
            columns = columns,
            tableData = tableData,
            onRowClick = onRowClick,
            strings = RussianStringProvider,
            customization = customization,
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
            horizontalState = horizontalState,
        )

    }
}

private class StorageTableCustomization :
    TableCustomization<StorageProductUi, StorageProductField> {
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

@Suppress("LongParameterList")
@Composable
private fun PeriodSelectorRow(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onSearchNewData: () -> Unit,
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
                onClick = onSearchNewData,
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

private data class NegativeBatchItem(
    val productId: Int,
    val itemId: Int,
    val displayName: String
)

private fun getNegativeBatches(tableData: StorageTableData): List<NegativeBatchItem> {
    return tableData.displayedProducts
        .mapNotNull { item ->
            // Только партии (isProduct = false)
            if (item.isProduct) return@mapNotNull null

            val hasNegative = item.balanceBeforeStart < 0 ||
                    item.incoming < 0 ||
                    item.outgoing < 0 ||
                    item.balanceOnEnd < 0

            if (hasNegative) {
                val displayName = "${item.productName} — ${item.itemName}"
                NegativeBatchItem(item.productId, item.itemId, displayName)
            } else null
        }
}

@Composable
private fun NegativeBatchesCard(
    negativeBatches: ImmutableList<NegativeBatchItem>,
    onBatchClick: (productId: Int, itemId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Отрицательные остатки: ${negativeBatches.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = negativeBatches,
                    key = { it.itemId }
                ) { batch ->
                    NegativeBatchItemRow(
                        item = batch,
                        onClick = { onBatchClick(batch.productId, batch.itemId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NegativeBatchItemRow(
    item: NegativeBatchItem,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(32.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 8.dp,
            vertical = 4.dp
        )
    ) {
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

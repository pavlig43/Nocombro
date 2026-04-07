package ru.pavlig43.immutable.internal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.component.ItemListState
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.export.ExcelColumn
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.export.TableExportFormat
import ru.pavlig43.tablecore.export.exportExcelFile
import ru.pavlig43.tablecore.export.formatValue
import ru.pavlig43.tablecore.ui.TableBox
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : IMultiLineTableUi, C> ImmutableTableBox(
    component: ImmutableTableComponent<*, I, C>,
    modifier: Modifier = Modifier
) {
    val itemListState by component.itemListState.collectAsState()

    val tableData: TableData<I> by component.tableData.collectAsState()

    Box(modifier.padding(16.dp)) {

        when (val state = itemListState) {

            is ItemListState.Error -> ErrorScreen(state.message)
            is ItemListState.Loading -> LoadingUi()
            is ItemListState.Success -> {
                TableBox(
                    columns = component.columns,
                    onFiltersChanged = component::updateFilters,
                    onSortChanged = component::updateSort,
                    initialSort = component.sort,
                    initialFilters = component.filters,
                ) { verticalState, horizontalState, tableState, stringProvider, modifier ->
                    ImmutableTable(
                        columns = component.columns,
                        tableState = tableState,
                        stringProvider = stringProvider,
                        verticalState = verticalState,
                        horizontalState = horizontalState,
                        items = tableData.displayedItems,
                        onEvent = component::onEvent,
                        onRowClick = { component.onItemClick(it) },
                        tableData = tableData,
                        exportConfiguration = component.exportConfiguration,
                        modifier = modifier
                    )

                }

            }

        }
    }
}

@Suppress("LongParameterList", "LongMethod","MagicNumber")
@OptIn(ExperimentalTableApi::class)
@Composable
private fun <I : IMultiLineTableUi, C, E : TableData<I>> BoxScope.ImmutableTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    tableState: TableState<C>,
    stringProvider: StringProvider,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    items: List<I>,
    onEvent: (ImmutableTableUiEvent) -> Unit,
    onRowClick: (I) -> Unit,
    tableData: E,
    exportConfiguration: TableExportConfiguration<I, C>?,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }
    var isExportMenuExpanded by remember { mutableStateOf(false) }
    // Оставляем место под верхнюю панель действий, чтобы она не слипалась с заголовком таблицы.
    val actionBarTopPadding = if (exportConfiguration != null) 132.dp else 0.dp
    val exportColumns = exportConfiguration?.let {
        buildExportColumns(
            columns = columns,
            tableState = tableState,
            items = items,
            exportConfiguration = it,
        )
    }

    Table(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = tableState,
        strings = stringProvider,
        customization = DefaultTableCustomization(),
        tableData = tableData,
        colors = TableDefaults.colors().copy(
            headerContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
        ),
        columns = columns,
        verticalState = verticalState,
        horizontalState = horizontalState,
        onRowClick = onRowClick,
        modifier = modifier.padding(top = actionBarTopPadding)
    )
    if (exportConfiguration != null && exportColumns != null) {
        ExportActionBar(
            exportConfiguration = exportConfiguration,
            isExportMenuExpanded = isExportMenuExpanded,
            onExpandExportMenu = { isExportMenuExpanded = true },
            onDismissExportMenu = { isExportMenuExpanded = false },
            onExportClick = { exportFormat ->
                coroutineScope.launch {
                    exportErrorMessage = runExport(
                        exportFormat = exportFormat,
                        exportConfiguration = exportConfiguration,
                        exportColumns = exportColumns,
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        exportErrorMessage?.let { message ->
            ValidationErrorsCard(
                errorMessages = listOf(message),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 138.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
    SelectionActionBar(
        selectedCount = tableData.selectedIds.size,
        onDeleteClick = {
            onEvent(ImmutableTableUiEvent.DeleteSelected)
        },
        onClearSelection = {
            onEvent(ImmutableTableUiEvent.Selection(SelectionUiEvent.ClearSelection))
        },
//                                liquidState = liquidState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
    )


}

@Composable
private fun <I : IMultiLineTableUi, C, E : TableData<I>> buildExportColumns(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    tableState: TableState<C>,
    items: List<I>,
    exportConfiguration: TableExportConfiguration<I, C>,
): List<ExcelColumn> {
    val visibleByKey = columns.filter { it.visible }.associateBy { it.key }
    return buildList {
        for (key in tableState.columnOrder) {
            val spec = visibleByKey[key] ?: continue
            // Заголовок колонки библиотека отдает через composable-провайдер,
            // поэтому сборщик колонок экспорта тоже должен оставаться в composable-контексте.
            val title = spec.title?.invoke()?.takeIf { it.isNotBlank() } ?: continue
            add(
                ExcelColumn(
                    header = title,
                    values = items.map { item ->
                        exportConfiguration.formatValue(
                            column = key,
                            rawValue = spec.valueOf(item),
                            item = item,
                        )
                    },
                ),
            )
        }
    }
}

@Composable
private fun BoxScope.ExportActionBar(
    exportConfiguration: TableExportConfiguration<*, *>,
    isExportMenuExpanded: Boolean,
    onExpandExportMenu: () -> Unit,
    onDismissExportMenu: () -> Unit,
    onExportClick: (TableExportFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Держим действия в отдельной панели, чтобы новые кнопки уровня таблицы
    // можно было добавлять без разрастания основной composable-функции таблицы.
    Surface(
        modifier = modifier.border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            ),
            shape = RoundedCornerShape(22.dp),
        ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.wrapContentWidth(),
                ) {
                    FilledTonalButton(
                        onClick = onExpandExportMenu,
                        enabled = true,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Экспорт")
                    }
                    DropdownMenu(
                        expanded = isExportMenuExpanded,
                        onDismissRequest = onDismissExportMenu,
                    ) {
                        TableExportFormat.entries.forEach { exportFormat ->
                            val exportEnabled = exportFormat in exportConfiguration.supportedFormats
                            DropdownMenuItem(
                                text = { Text(exportFormat.label()) },
                                onClick = {
                                    onDismissExportMenu()
                                    if (exportEnabled) onExportClick(exportFormat)
                                },
                                enabled = exportEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun runExport(
    exportFormat: TableExportFormat,
    exportConfiguration: TableExportConfiguration<*, *>,
    exportColumns: List<ExcelColumn>,
): String? =
    when (exportFormat) {
        TableExportFormat.Excel -> {
            // UI уже ходит через общее меню форматов,
            // поэтому для PDF/Word потом достаточно расширить этот диспетчер.
            val result = exportExcelFile(
                suggestedFileName = exportConfiguration.suggestedFileName,
                columns = exportColumns,
            )
            result.exceptionOrNull()?.message ?: "Не удалось экспортировать таблицу в Excel."
                .takeIf { result.isFailure }
        }

        TableExportFormat.Pdf,
        TableExportFormat.Word -> null
    }

private fun TableExportFormat.label(): String =
    when (this) {
        TableExportFormat.Excel -> "Excel (.xlsx)"
        TableExportFormat.Pdf -> "PDF"
        TableExportFormat.Word -> "Word"
    }

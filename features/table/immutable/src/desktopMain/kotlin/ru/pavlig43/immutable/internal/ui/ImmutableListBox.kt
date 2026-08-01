package ru.pavlig43.immutable.internal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.component.ItemListState
import ru.pavlig43.immutable.internal.component.manager.DeleteState
import ru.pavlig43.tablecore.export.ExcelColumn
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.export.TableExportFormat
import ru.pavlig43.tablecore.export.exportExcelFile
import ru.pavlig43.tablecore.export.formatValue
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.TableBox
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.ColumnWidthAction
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : IMultiLineTableUi, C> ImmutableTableBox(
    component: ImmutableTableComponent<*, I, C>,
    modifier: Modifier = Modifier,
) {
    val itemListState by component.itemListState.collectAsState()
    val deleteState by component.deleteState.collectAsState()
    val tableData: TableData<I> by component.tableData.collectAsState()
    val searchQuery by component.searchQuery.collectAsState()

    val rootFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<TableExportFormat?>(null) }
    var exportErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    TableSearchKeyboardHandler(
        owner = component,
        query = searchQuery,
        rootFocused = rootFocused,
        searchFocused = searchFocused,
        rootFocusRequester = rootFocusRequester,
        searchFocusRequester = searchFocusRequester,
        onQueryChange = component::updateSearchQuery,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .focusRequester(rootFocusRequester)
            .onFocusChanged { rootFocused = it.isFocused }
            .focusable(),
    ) {
        ImmutableTableToolbar(
            searchQuery = searchQuery,
            onSearchQueryChange = component::updateSearchQuery,
            onCreateClick = { component.onEvent(ImmutableTableUiEvent.CreateNewItem) },
            showCreateAction = component.showCreateAction,
            exportConfiguration = component.exportConfiguration,
            exportEnabled = tableData.displayedItems.isNotEmpty(),
            onExportClick = { pendingExportFormat = it },
            searchFocusRequester = searchFocusRequester,
            onSearchFocusChanged = { searchFocused = it },
        )

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                when (val state = itemListState) {
                    is ItemListState.Error -> ErrorScreen(state.message)
                    is ItemListState.Loading -> LoadingUi()
                    is ItemListState.Success -> {
                        if (state.data.isEmpty()) {
                            TableEmptyState("Записей пока нет")
                        } else {
                            ImmutableTable(
                                columns = component.columns,
                                items = tableData.displayedItems,
                                onEvent = component::onEvent,
                                onRowClick = component.onItemClick,
                                onFiltersChanged = component::updateFilters,
                                onSortChanged = component::updateSort,
                                initialFilters = component.filters,
                                initialSort = component.sort,
                                tableData = tableData,
                                deleteState = deleteState,
                                searchQuery = searchQuery,
                                exportConfiguration = component.exportConfiguration,
                                pendingExportFormat = pendingExportFormat,
                                onExportStarted = { pendingExportFormat = null },
                                onExportFinished = { exportErrorMessage = it },
                                exportErrorMessage = exportErrorMessage,
                                availableWidth = maxWidth,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TableEmptyState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Center),
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTableApi::class)
private fun <I : IMultiLineTableUi, C, E : TableData<I>> BoxScope.ImmutableTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    items: List<I>,
    onEvent: (ImmutableTableUiEvent) -> Unit,
    onRowClick: (I) -> Unit,
    onFiltersChanged: (Map<C, TableFilterState<*>>) -> Unit,
    onSortChanged: (SortState<C>?) -> Unit,
    initialFilters: Map<C, TableFilterState<*>>,
    initialSort: SortState<C>?,
    tableData: E,
    deleteState: DeleteState,
    searchQuery: String,
    exportConfiguration: TableExportConfiguration<I, C>?,
    pendingExportFormat: TableExportFormat?,
    onExportStarted: () -> Unit,
    onExportFinished: (String?) -> Unit,
    exportErrorMessage: String?,
    availableWidth: Dp,
) {
    val uiContext = ImmutableTableUiContext(
        searchQuery = searchQuery,
        displayedIds = items.mapTo(mutableSetOf()) { it.composeId },
        selectedIds = tableData.selectedIds,
        selectionEnabled = tableData.isSelectionMode,
    )

    CompositionLocalProvider(LocalImmutableTableUiContext provides uiContext) {
        TableBox(
            columns = columns,
            onFiltersChanged = onFiltersChanged,
            onSortChanged = onSortChanged,
            initialSort = initialSort,
            initialFilters = initialFilters,
            modifier = Modifier.fillMaxSize(),
            dimensions = TableDefaults.compactDimensions().copy(
                rowHeight = 44.dp,
                headerHeight = 48.dp,
                footerHeight = 44.dp,
            ),
            tableModifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp, bottom = 12.dp),
            subtleScrollbars = true,
            tableSettingsModify = { settings ->
                settings.copy(
                    stripedRows = true,
                    enableTextSelection = false,
                )
            },
        ) { verticalState, horizontalState, tableState, stringProvider, tableModifier ->
            val exportColumns = exportConfiguration?.let {
                buildExportColumns(
                    columns = columns,
                    tableState = tableState,
                    items = items,
                    exportConfiguration = it,
                )
            }
            FillTableWidthEffect(
                tableState = tableState,
                columns = columns,
                availableWidth = availableWidth - 12.dp,
            )
            LaunchedEffect(pendingExportFormat, exportColumns) {
                val format = pendingExportFormat ?: return@LaunchedEffect
                val configuration = exportConfiguration ?: return@LaunchedEffect
                val columnsForExport = exportColumns ?: return@LaunchedEffect
                onExportStarted()
                onExportFinished(
                    runExport(
                        exportFormat = format,
                        exportConfiguration = configuration,
                        exportColumns = columnsForExport,
                    ),
                )
            }
            ImmutableTableContent(
                columns = columns,
                tableState = tableState,
                stringProvider = stringProvider,
                verticalState = verticalState,
                horizontalState = horizontalState,
                items = items,
                onRowClick = onRowClick,
                tableData = tableData,
                modifier = tableModifier,
            )
        }
    }

    if (items.isEmpty()) {
        TableEmptyState("Ничего не найдено")
    }
    SelectionActionBar(
        selectedCount = tableData.selectedIds.size,
        deleteState = deleteState,
        onDeleteClick = { onEvent(ImmutableTableUiEvent.DeleteSelected) },
        onClearSelection = {
            onEvent(ImmutableTableUiEvent.Selection(SelectionUiEvent.ClearSelection))
        },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
    )
    exportErrorMessage?.let { message ->
        ValidationErrorsCard(
            errorMessages = listOf(message),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )
    }
    if (deleteState is DeleteState.Error) {
        ValidationErrorsCard(
            errorMessages = listOf("Не удалось удалить строки: ${deleteState.message}"),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp),
        )
    }
}

@Composable
private fun <I : IMultiLineTableUi, C, E : TableData<I>> FillTableWidthEffect(
    tableState: TableState<C>,
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    availableWidth: Dp,
) {
    LaunchedEffect(tableState, availableWidth) {
        repeat(3) { withFrameNanos { } }
        val visibleColumns = tableState.columnOrder.mapNotNull { key ->
            columns.firstOrNull { it.key == key && it.visible }
        }
        if (visibleColumns.isEmpty()) return@LaunchedEffect

        val extraWidth = availableWidth - tableState.tableWidth
        if (extraWidth <= 0.dp) return@LaunchedEffect

        val expandableColumns = visibleColumns.filterNot { column ->
            column.minWidth == 48.dp && column.autoMaxWidth == 48.dp
        }.ifEmpty { visibleColumns }
        val widthPerColumn = extraWidth / expandableColumns.size
        expandableColumns.forEach { column ->
            tableState.resizeColumn(
                column = column.key,
                action = ColumnWidthAction.Set(
                    tableState.resolveColumnWidth(column.key, column) + widthPerColumn,
                ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalTableApi::class)
private fun <I : IMultiLineTableUi, C, E : TableData<I>> ImmutableTableContent(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    tableState: TableState<C>,
    stringProvider: StringProvider,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    items: List<I>,
    onRowClick: (I) -> Unit,
    tableData: E,
    modifier: Modifier,
) {
    val selectedRowColor = lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.tertiaryContainer,
        0.07f,
    )
    val selectedRowBorderColor =
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)

    Table(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        rowKey = { item, index -> item?.composeId ?: index },
        state = tableState,
        strings = stringProvider,
        customization = object : TableCustomization<I, C> {
            @Composable
            override fun resolveRowStyle(ctx: TableRowContext<I, C>): TableRowStyle =
                if (ctx.item.composeId in tableData.selectedIds) {
                    TableRowStyle(
                        containerColor = selectedRowColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, selectedRowBorderColor),
                    )
                } else {
                    TableRowStyle()
                }
        },
        tableData = tableData,
        colors = TableDefaults.colors(
            headerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            rowContainerColor = MaterialTheme.colorScheme.surface,
            rowSelectedContainerColor = selectedRowColor,
            stripedRowContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            footerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        columns = columns,
        verticalState = verticalState,
        horizontalState = horizontalState,
        onRowClick = onRowClick,
        shape = RectangleShape,
        border = TableDefaults.NoBorder,
        modifier = modifier,
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

private suspend fun runExport(
    exportFormat: TableExportFormat,
    exportConfiguration: TableExportConfiguration<*, *>,
    exportColumns: List<ExcelColumn>,
): String? = when (exportFormat) {
    TableExportFormat.Excel -> {
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

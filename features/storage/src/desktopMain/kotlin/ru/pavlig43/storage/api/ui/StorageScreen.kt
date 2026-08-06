package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.datetime.period.dateTime.VerticalDateTimeSelectorScreen
import ru.pavlig43.immutable.internal.ui.TableSearchKeyboardHandler
import ru.pavlig43.storage.api.component.storage.LoadState
import ru.pavlig43.storage.api.component.storage.StorageComponent
import ru.pavlig43.storage.api.component.storage.StorageProductField
import ru.pavlig43.storage.api.component.storage.createStorageColumns
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.export.ExportCellValue
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.export.defaultExportValue
import ru.pavlig43.tablecore.export.formatValue
import ru.pavlig43.tablecore.state.rememberSaveableTableState
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ru.pavlig43.theme.Res
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

/** Минимальная ширина, при которой элементы боковой панели не сжимаются. */
private val StorageSidePanelMinWidth = 360.dp

/** Максимальная ширина боковой панели на широком окне приложения. */
private val StorageSidePanelMaxWidth = 420.dp

/**
 * Показывает экран склада и обслуживает диалоги операций над партиями.
 *
 * Основная область содержит таблицу с поиском, а правая панель постоянно
 * занимает одно место: сверху настройки, снизу прокручиваемый список ошибок.
 */
@Suppress("LongMethod")
@Composable
fun StorageScreen(
    component: StorageComponent
) {
    val storageLocation by component.storageLocation.collectAsState()
    val batchActions by component.batchActions.collectAsState()
    batchActions?.let { state ->
        StorageBatchActionsDialog(
            state = state,
            onDismiss = component::onDismissBatchActions,
            onHistory = component::onOpenBatchHistory,
            onTransfer = component::onOpenTransfer,
            onWriteOff = component::onOpenWriteOff,
        )
    }

    val operationDialog by component.operationDialog.collectAsState()
    operationDialog?.let { state ->
        StorageOperationDialog(
            state = state,
            onDismiss = component::onDismissOperation,
            onCountChange = component::onUpdateOperationCount,
            onDateTimeChange = component::onUpdateOperationDateTime,
            onReasonChange = component::onUpdateOperationReason,
            onCommentChange = component::onUpdateOperationComment,
            onSubmit = component::onSubmitOperation,
        )
    }

    val loadState by component.loadState.collectAsState()
    val tableData by component.tableData.collectAsState()
    val negativeBatchRows by component.negativeBatches.collectAsState()
    val searchQuery by component.searchQuery.collectAsState()

    StorageContent(
        component = component,
        storageLocation = storageLocation,
        loadState = loadState,
        tableData = tableData,
        negativeBatches = remember(negativeBatchRows) {
            getNegativeBatches(negativeBatchRows).toImmutableList()
        },
        searchQuery = searchQuery,
    )
}

/**
 * Связывает состояние таблицы, поиск и переход из ошибки к нужной партии.
 *
 * При выборе ошибки запрос поиска очищается, товар раскрывается, после чего
 * таблица прокручивается до соответствующей строки партии.
 */
@Composable
@Suppress("LongMethod")
private fun StorageContent(
    component: StorageComponent,
    storageLocation: StorageLocation,
    loadState: LoadState,
    tableData: StorageTableData,
    negativeBatches: ImmutableList<NegativeBatchItem>,
    searchQuery: String,
) {
    val currentSearchQuery = rememberUpdatedState(searchQuery)
    val columns = remember {
        createStorageColumns(
            onToggleExpand = component::toggleExpand,
            onToggleExpandAll = component::toggleExpandAll,
            onOpenProduct = component::openProduct,
            searchQuery = { currentSearchQuery.value },
        )
    }
    val tableSettings = remember {
        TableSettings(
            showActiveFiltersHeader = true,
            enableTextSelection = true,
            enableDragToScroll = true,
        )
    }
    val tableState = rememberSaveableTableState(
        columns = StorageProductField.entries.toImmutableList(),
        settings = tableSettings,
    )
    LaunchedEffect(tableState) {
        snapshotFlow { tableState.filters.toMap() }.collect(component::updateFilters)
    }
    val verticalState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val rootFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }

    TableSearchKeyboardHandler(
        owner = component,
        query = searchQuery,
        rootFocused = rootFocused,
        searchFocused = searchFocused,
        rootFocusRequester = rootFocusRequester,
        searchFocusRequester = searchFocusRequester,
        onQueryChange = component::updateSearchQuery,
    )

    val onNegativeBatchClick: (Int, Int) -> Unit = { productId, itemId ->
        coroutineScope.launch {
            component.updateSearchQuery("")
            component.expandProduct(productId)
            val currentData = component.tableData.first { data ->
                data.displayedProducts.any { item ->
                    item.isProduct && item.productId == productId && item.isExpanded
                }
            }
            val newIndex = currentData.displayedProducts.indexOfFirst { item ->
                !item.isProduct && item.productId == productId && item.itemId == itemId
            }
            if (newIndex >= 0) {
                verticalState.animateScrollToItem(index = newIndex, scrollOffset = -80)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .onFocusChanged { rootFocused = it.isFocused }
            .focusable(),
    ) {
        val sidePanelWidth = (maxWidth * 0.24f).coerceIn(
            StorageSidePanelMinWidth,
            StorageSidePanelMaxWidth,
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (loadState) {
                    is LoadState.Error -> ErrorScreen(loadState.message)
                    is LoadState.Loading -> LoadingUi()
                    is LoadState.Success -> StorageTable(
                        state = tableState,
                        tableData = tableData,
                        columns = columns,
                        verticalState = verticalState,
                        onRowClick = component::onRowClick,
                        searchQuery = searchQuery,
                        onSearchQueryChange = component::updateSearchQuery,
                        searchFocusRequester = searchFocusRequester,
                        onSearchFocusChanged = { searchFocused = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            StorageSidePanel(
                component = component,
                storageLocation = storageLocation,
                negativeBatches = negativeBatches,
                onBatchClick = onNegativeBatchClick,
                modifier = Modifier
                    .width(sidePanelWidth)
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 24.dp, bottom = 24.dp),
            )
        }
    }
}

/**
 * Отображает таблицу склада с общей панелью поиска и экспорта.
 *
 * Поиск передаётся снаружи, чтобы его состояние оставалось в [StorageComponent]
 * и могло быть сброшено при переходе к партии из списка ошибок.
 */
@OptIn(ExperimentalTableApi::class)
@Suppress("LongMethod", "LongParameterList", "MagicNumber")
@Composable
private fun StorageTable(
    state: TableState<StorageProductField>,
    tableData: StorageTableData,
    columns: ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, StorageTableData>>,
    verticalState: LazyListState,
    onRowClick: (StorageProductUi) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var exportErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isExportMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val customization = remember { StorageTableCustomization() }
    val exportConfiguration = remember {
        TableExportConfiguration<StorageProductUi, StorageProductField>(
            suggestedFileName = "storage-export",
        ) { column, rawValue, item ->
            when (column) {
                StorageProductField.NAME -> {
                    val displayName = if (item.isProduct) item.itemName else "    ${item.itemName}"
                    ExportCellValue.Text(displayName)
                }

                StorageProductField.VENDOR_NAME -> ExportCellValue.Text(item.vendorNames)
                StorageProductField.BALANCE_BEFORE -> defaultExportValue(DecimalData3(item.balanceBeforeStart))
                StorageProductField.INCOMING -> defaultExportValue(DecimalData3(item.incoming))
                StorageProductField.OUTGOING -> defaultExportValue(DecimalData3(item.outgoing))
                StorageProductField.BALANCE_END -> defaultExportValue(DecimalData3(item.balanceOnEnd))
                StorageProductField.EXPAND -> defaultExportValue(rawValue)
            }
        }
    }
    val exportColumns = buildStorageExportColumns(
        columns = columns,
        tableState = state,
        items = tableData.displayedProducts,
        exportConfiguration = exportConfiguration,
    )
    val actionBarTopPadding = if (exportErrorMessage == null) 96.dp else 168.dp

    Box(
        modifier = modifier.padding(start = 24.dp, bottom = 24.dp),
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
            colors = TableDefaults.colors(
                headerContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxSize().padding(top = actionBarTopPadding),

        )
        StorageExportActionBar(
            exportConfiguration = exportConfiguration,
            isExportMenuExpanded = isExportMenuExpanded,
            onExpandExportMenu = { isExportMenuExpanded = true },
            onDismissExportMenu = { isExportMenuExpanded = false },
            onExportClick = { exportFormat ->
                coroutineScope.launch {
                    exportErrorMessage = runStorageExport(
                        exportFormat = exportFormat,
                        exportConfiguration = exportConfiguration,
                        exportColumns = exportColumns,
                    )
                }
            },
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            searchFocusRequester = searchFocusRequester,
            onSearchFocusChanged = onSearchFocusChanged,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(end = 24.dp, top = 12.dp),
        )
        exportErrorMessage?.let { message ->
            ValidationErrorsCard(
                errorMessages = listOf(message),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 96.dp, end = 24.dp),
            )
        }
    }
}

/** Выделяет партии и отрицательные значения средствами оформления таблицы. */
private class StorageTableCustomization :
    TableCustomization<StorageProductUi, StorageProductField> {
    @Composable
    override fun resolveRowStyle(ctx: TableRowContext<StorageProductUi, StorageProductField>): TableRowStyle {
        return when {
            !ctx.item.isProduct && ctx.item.hasNegativeBalanceHistory -> TableRowStyle(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                elevation = 2.dp,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
            )

            !ctx.item.isProduct -> TableRowStyle(
                contentColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = 2.dp,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            )

            else -> TableRowStyle()
        }
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

/** Краткие данные ошибки остатка, необходимые правой панели для навигации. */
private data class NegativeBatchItem(
    val productId: Int,
    val itemId: Int,
    val displayName: String
)

/** Преобразует строки партий с отрицательным остатком в элементы панели ошибок. */
private fun getNegativeBatches(items: List<StorageProductUi>): List<NegativeBatchItem> {
    return items
        .mapNotNull { item ->
            // Только партии (isProduct = false)
            if (item.isProduct) return@mapNotNull null

            if (item.hasNegativeBalanceHistory) {
                val displayName = "${item.itemName} — ${item.productName}"
                NegativeBatchItem(item.productId, item.itemId, displayName)
            } else null
        }
}

/**
 * Рисует постоянную правую панель экрана склада.
 *
 * Блок настроек имеет собственную естественную высоту, а карточка ошибок
 * занимает всё оставшееся место и прокручивает только своё содержимое.
 */
@Composable
private fun StorageSidePanel(
    component: StorageComponent,
    storageLocation: StorageLocation,
    negativeBatches: ImmutableList<NegativeBatchItem>,
    onBatchClick: (productId: Int, itemId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                VerticalDateTimeSelectorScreen(
                    component = component.dTPeriodComponent,
                    modifier = Modifier.fillMaxWidth(),
                )
                StorageLocationSelector(
                    selected = storageLocation,
                    onSelect = component::onSelectStorageLocation,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        NegativeBatchesCard(
            negativeBatches = negativeBatches,
            onBatchClick = onBatchClick,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

/** Показывает количество ошибок остатков и независимо прокручиваемый список партий. */
@Composable
private fun NegativeBatchesCard(
    negativeBatches: ImmutableList<NegativeBatchItem>,
    onBatchClick: (productId: Int, itemId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                    "Ошибки остатков",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = negativeBatches.size.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            if (negativeBatches.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Ошибок остатков нет",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = negativeBatches,
                            key = { it.itemId },
                        ) { batch ->
                            NegativeBatchItemRow(
                                item = batch,
                                onClick = { onBatchClick(batch.productId, batch.itemId) },
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/** Показывает одну кликабельную ошибку и передаёт навигацию вызывающему коду. */
@Composable
private fun NegativeBatchItemRow(
    item: NegativeBatchItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(4.dp).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error),
            )
            Text(
                text = item.displayName,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

package ru.pavlig43.product.internal.update.tabs.declaration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ProjectDialog
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ru.pavlig43.tablecore.ui.ScrollBar
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.close
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState

@Composable
internal fun DeclarationScreen(
    component: ProductDeclarationComponent
) {
    val dialog by component.dialog.subscribeAsState()
    val parseSheetUi by component.currentParseSheetUi.collectAsState()
    val parseProgressUi by component.currentParseProgressUi.collectAsState()
    val parseResultUi by component.currentParseResultUi.collectAsState()
    ProductDeclarationScreen(component)

    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }

    parseSheetUi?.let { sheetUi ->
        ParseDeclarationFilesSheet(
            ui = sheetUi,
            onDismiss = component::dismissParseSheet,
            onPdfFilterChange = component::setPdfFilter,
            onFileClick = component::parseSelectedFile,
        )
    }

    parseProgressUi?.let { progressUi ->
        ParseProgressDialog(progressUi)
    }

    parseResultUi?.let { resultUi ->
        ParseResultDialog(
            ui = resultUi,
            onDismiss = component::dismissParseResult,
        )
    }
}
@OptIn(ExperimentalTableApi::class)
@Composable
private fun ProductDeclarationScreen(
    component: ProductDeclarationComponent,
    modifier: Modifier = Modifier
) {

    LoadInitDataScreen(component.initDataComponent) {

        val tableData by component.tableData.collectAsState()
        ProductDeclarationTable(
            columns = component.columns,
            items = tableData.displayedItems,
            onEvent = component::onEvent,
            tableData = tableData,
            modifier = modifier
        )

    }
}

@Suppress("LongParameterList", "LongMethod","MagicNumber")
@OptIn(ExperimentalTableApi::class)
@Composable
private fun ProductDeclarationTable(
    columns: ImmutableList<ColumnSpec<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>>>,
    items: List<ProductDeclarationTableUi>,
    onEvent: (ProductDeclarationEvent) -> Unit,
    tableData: TableData<ProductDeclarationTableUi>,
    modifier: Modifier
) {

    val defaultTableSettings = TableSettings(
        stripedRows = true,
        autoApplyFilters = false,
        rowHeightMode = RowHeightMode.Dynamic,
        selectionMode = SelectionMode.Multiple,
    )
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()

    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = defaultTableSettings
    )
    Box(modifier.fillMaxSize()){
        Table(
            itemsCount = items.size,
            itemAt = { index -> items.getOrNull(index) },
            state = state,
            strings = RussianStringProvider,
            onRowClick = {
                if (!it.isProductInDeclaration) {
                    onEvent(ProductDeclarationEvent.OpenDeclaration(it.declarationId))
                }
            },
            tableData = tableData,
            colors = TableDefaults.colors().copy(
                headerContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
            ),
            columns = columns,
            verticalState = verticalState,
            horizontalState = horizontalState,
            modifier = modifier
        )
        SelectionActionBar(
            selectedCount = tableData.selectedIds.size,
            onDeleteClick = {
                onEvent(ProductDeclarationEvent.DeleteSelected)
            },
            onClearSelection = {
                onEvent(ProductDeclarationEvent.Selection(SelectionUiEvent.ClearSelection))
            },
//                                liquidState = liquidState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
        ScrollBar(
            verticalState,horizontalState
        )
    }

}

@Composable
private fun ParseDeclarationFilesSheet(
    ui: ParseDeclarationSheetUi,
    onDismiss: () -> Unit,
    onPdfFilterChange: (Boolean) -> Unit,
    onFileClick: (String) -> Unit,
) {
    val displayedFiles = if (ui.onlyPdf) {
        ui.files.filter(ParseFileUi::isSupportedForParsing)
    } else {
        ui.files
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Файлы декларации ${ui.declarationName}",
                style = MaterialTheme.typography.titleLarge,
            )
            FilterChip(
                selected = ui.onlyPdf,
                onClick = { onPdfFilterChange(!ui.onlyPdf) },
                label = {
                    Text(if (ui.onlyPdf) "PDF и PNG" else "Все файлы")
                },
            )

            if (displayedFiles.isEmpty()) {
                Text(
                    text = if (ui.onlyPdf) {
                        "У декларации нет PDF или PNG файлов."
                    } else {
                        "У декларации нет файлов."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayedFiles, key = ParseFileUi::path) { fileUi ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFileClick(fileUi.path) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = fileUi.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = when {
                                        fileUi.isPdf -> "PDF"
                                        fileUi.isPng -> "PNG"
                                        else -> "Не поддерживается"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParseProgressDialog(
    ui: ParseProgressUi,
) {
    ProjectDialog(
        onDismissRequest = {},
        header = {
            Text("Парсинг")
        },
        content = {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LoadingUi(Modifier.size(48.dp))
                Text(
                    text = "Проверяю файл ${ui.fileName} для декларации ${ui.declarationName}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
    )
}

@Composable
private fun ParseResultDialog(
    ui: ParseResultUi,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = ui.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = ui.message,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(if (ui.isMatch) "Отлично" else "Понятно")
                }
            }
        }
    }
}


@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onDeleteClick: () -> Unit,
    onClearSelection: () -> Unit,
//    liquidState: LiquidState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        // Liquid Glass effect: GPU-accelerated shader distortion with semi-transparent background
        Surface(
            modifier = Modifier,
//                .liquid(liquidState),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            shape = RoundedCornerShape(24.dp),
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = "Clear selection",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDeleteClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Delete")
                }
            }
        }
    }
}

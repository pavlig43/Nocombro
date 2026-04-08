package ru.pavlig43.product.internal.update.tabs.declaration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
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
    ProductDeclarationScreen(component)

    dialog.child?.instance?.also {
        MBSImmutableTable(it)
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

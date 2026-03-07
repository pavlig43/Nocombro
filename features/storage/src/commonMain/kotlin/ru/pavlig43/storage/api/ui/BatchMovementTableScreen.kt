package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.datetime.period.dateTime.DateTimeSelectorScreen
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementComponent
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementField
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementLoadState
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementTableUi
import ru.pavlig43.storage.api.component.batchMovement.createBatchMovementColumns
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ru.pavlig43.tablecore.ui.ScrollBar
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState

@OptIn(ExperimentalTableApi::class)
@Composable
fun BatchMovementTableScreen(
    component: BatchMovementComponent,
    modifier: Modifier = Modifier
) {


    DateTimeSelectorScreen(
        component.dateTimeComponent
    )

    val loadState by component.loadState.collectAsState()

    when (val state = loadState) {
        is BatchMovementLoadState.Error -> ErrorScreen(state.message)
        is BatchMovementLoadState.Loading -> LoadingUi()
        is BatchMovementLoadState.Success -> {
            // Card с информацией о продукте и партии
            BatchInfoCard(
                productName = state.info.productName,
                batchName = state.info.batchName,
            )

            val columns = remember { createBatchMovementColumns() }
            val tableState = rememberTableState(
                columns = BatchMovementField.entries.toImmutableList()
            )
            val verticalState = rememberLazyListState()

            BatchMovementTable(
                state = tableState,
                items = state.info.movements,
                columns = columns,
                verticalState = verticalState,
                onRowClick = component::onRowClick,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalTableApi::class)
@Suppress("LongParameterList")
@Composable
private fun BatchMovementTable(
    state: TableState<BatchMovementField>,
    items:List<BatchMovementTableUi>,
    columns: ImmutableList<ColumnSpec<BatchMovementTableUi, BatchMovementField, Unit>>,
    verticalState: LazyListState,
    onRowClick: (BatchMovementTableUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalState = rememberScrollState()
    val customization = remember { BatchMovementTableCustomization() }


    Box(
        modifier = modifier.padding(start = 24.dp)
    ) {
        Table(
            itemsCount = items.size,
            itemAt = { index -> items.getOrNull(index) },
            state = state,
            columns = columns,
            tableData = Unit,
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
            horizontalState = horizontalState
        )
    }
}

@Composable
private fun BatchInfoCard(
    productName: String,
    batchName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp, end = 24.dp),
        shape = RoundedCornerShape(8.dp),
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
        }
    }
}

private class BatchMovementTableCustomization :
    TableCustomization<BatchMovementTableUi, BatchMovementField> {

    @Composable
    override fun resolveRowStyle(ctx: TableRowContext<BatchMovementTableUi, BatchMovementField>): TableRowStyle {
        // Все строки одного типа — нет необходимости в стилизации по isProduct
        return TableRowStyle()
    }

    @Composable
    override fun resolveCellStyle(ctx: TableCellContext<BatchMovementTableUi, BatchMovementField>): TableCellStyle {
        val item = ctx.row.item
        val cellValue = when (ctx.column) {
            BatchMovementField.BALANCE_BEFORE -> item.balanceBeforeStart
            BatchMovementField.INCOMING -> item.incoming
            BatchMovementField.OUTGOING -> item.outgoing
            BatchMovementField.BALANCE_END -> item.balanceOnEnd
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

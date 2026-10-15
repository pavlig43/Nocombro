package ru.pavlig43.transaction.internal.multiopzs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineDateTimeField
import ru.pavlig43.mutable.api.multiLine.ui.SelectionActionBar
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.TableBox
import ua.wwind.table.EditableTable
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults

@Composable
internal fun MultiOpzsScreen(component: MultiOpzsComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    val dialog by component.dialog.subscribeAsState()
    val columns = remember(component, state.running) { multiOpzsColumns(component, state.running) }
    val tableData = TableData(
        displayedItems = state.rows, selectedIds = state.selectedIds, isSelectionMode = !state.running,
    )

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("МультиОПЗС", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Button(
                    component::create,
                    enabled = !state.running && state.validationErrors.isEmpty() && state.rows.none { it.loading } &&
                        state.rows.any { it.result !is OpzsRowResult.Created },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp),
                ) { Text("Создать ОПЗС") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Дата и время производства")
            SingleLineDateTimeField(state.producedAt, component::openDate, readOnly = state.running)
            if (state.finished) Text("Создано и проведено: ${state.createdCount} из ${state.rows.size}")
        }
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(Modifier.fillMaxSize()) {
                TableBox(
                    columns = columns,
                    onFiltersChanged = {},
                    onSortChanged = {},
                    modifier = Modifier.fillMaxSize(),
                    dimensions = TableDefaults.compactDimensions().copy(headerHeight = 48.dp),
                    tableModifier = Modifier.fillMaxSize().padding(end = 12.dp, bottom = 12.dp),
                    subtleScrollbars = true,
                    tableSettingsModify = { settings -> settings.copy(
                        selectionMode = SelectionMode.None,
                        enableTextSelection = false,
                        showFooter = false,
                        rowHeightMode = RowHeightMode.Dynamic,
                    ) },
                ) { verticalState, horizontalState, tableState, strings, tableModifier ->
                    EditableTable(
                        itemsCount = state.rows.size,
                        itemAt = { state.rows.getOrNull(it) },
                        rowKey = { row, index -> row?.composeId ?: index },
                        state = tableState,
                        strings = strings,
                        tableData = tableData,
                        columns = columns,
                        colors = TableDefaults.colors().copy(
                            headerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            rowContainerColor = MaterialTheme.colorScheme.surface,
                            stripedRowContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        border = TableDefaults.NoBorder,
                        verticalState = verticalState,
                        horizontalState = horizontalState,
                        modifier = tableModifier,
                    )
                }
                SelectionActionBar(
                    selectedCount = state.selectedIds.size,
                    onDeleteClick = { component.onTableEvent(MutableUiEvent.DeleteSelected) },
                    onClearSelection = {
                        component.onTableEvent(MutableUiEvent.Selection(SelectionUiEvent.ClearSelection))
                    },
                    selectedLabel = "Выбрано: ${state.selectedIds.size}",
                    deleteLabel = "Удалить",
                    clearSelectionDescription = "Снять выделение",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
        }
        if (state.errorMessages.isNotEmpty()) {
            ValidationErrorsCard(
                errorMessages = state.errorMessages,
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
            )
        }
    }
    when (val child = dialog.child?.instance) {
        is MultiOpzsDialogChild.DateTime -> DateTimePickerDialog(child.component)
        is MultiOpzsDialogChild.Table -> MBSImmutableTable(child.component)
        null -> Unit
    }
}

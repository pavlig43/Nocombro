package ru.pavlig43.transaction.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import ru.pavlig43.coreui.ClickableValidationErrorsCard
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.datetime.dateTimeWithSecondsFormat
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox
import ru.pavlig43.transaction.api.component.ProductPlanComponent
import ru.pavlig43.transaction.internal.plan.PlanValidationError
import ua.wwind.table.config.TableDefaults

@Composable
fun ProductPlanScreen(component: ProductPlanComponent) {
    val state by component.state.collectAsState()
    val rows = component.rows
    val loadState by rows.initDataComponent.loadState.collectAsState()
    val dirty by rows.dirty.collectAsState(initial = state.id == null)
    val createdAt by rows.createdAt.collectAsState(initial = null)
    val validationErrors by rows.validationErrors.collectAsState(initial = emptyList())
    val tableScroll = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val picker by component.productPicker.subscribeAsState()
    val loaded = loadState is LoadInitDataState.Success
    val editable = loaded && !state.busy
    val useSaved = editable && !dirty

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.id != null && loaded) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = component::print, enabled = useSaved) { Text("Раскладка") }
                OutlinedButton(onClick = component::openMultiOpzs, enabled = useSaved) { Text("МультиОПЗС") }
            }
        }
        if (state.busy) Text("Подождите…")
        createdAt?.let {
            Text(it.format(dateTimeWithSecondsFormat), style = MaterialTheme.typography.titleMedium)
        }
        MutableTableBox(
            component = rows,
            modifier = Modifier.fillMaxWidth().weight(1f),
            showExport = false,
            rowKey = { row, index -> row?.line?.syncId ?: index },
            dimensions = TableDefaults.compactDimensions().copy(rowHeight = 56.dp, headerHeight = 48.dp),
            tableSettingsModify = { it.copy(editingEnabled = editable, showFooter = false) },
            verticalState = tableScroll,
        )
        ProductPlanErrors(validationErrors, state.error) { rowKey ->
            coroutineScope.launch {
                val index = rows.tableData.value.displayedItems.indexOfFirst { it.line.syncId == rowKey }
                if (index >= 0) tableScroll.animateScrollToItem(index)
            }
        }
        if (loaded) {
            Button(
                onClick = component::save,
                enabled = editable && dirty && validationErrors.isEmpty(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Сохранить") }
        }
    }
    picker.child?.instance?.let { MBSImmutableTable(it) }
}

@Composable
private fun ProductPlanErrors(
    validationErrors: List<PlanValidationError>,
    actionError: String?,
    onRowClick: (String) -> Unit,
) {
    if (validationErrors.isEmpty() && actionError == null) return

    val rowErrors = validationErrors.mapNotNull { error ->
        error.rowKey?.let { rowKey -> error.message to { onRowClick(rowKey) } }
    }
    val formErrors = validationErrors.filter { it.rowKey == null }.map { it.message }
    val errors = formErrors + listOfNotNull(actionError?.takeUnless { message ->
        validationErrors.any { it.message == message }
    })

    Column(Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
        if (rowErrors.isNotEmpty()) {
            ClickableValidationErrorsCard(rowErrors, Modifier.fillMaxWidth())
        }
        if (errors.isNotEmpty()) {
            ValidationErrorsCard(errors, Modifier.fillMaxWidth())
        }
    }
}

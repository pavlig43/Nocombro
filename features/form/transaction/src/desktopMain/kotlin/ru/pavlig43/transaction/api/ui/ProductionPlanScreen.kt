package ru.pavlig43.transaction.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.datetime.format
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.transaction.api.component.ProductionPlanComponent
import ru.pavlig43.transaction.api.component.ProductionPlansComponent

@Composable
fun ProductionPlansScreen(component: ProductionPlansComponent) {
    val plans by component.plans.collectAsState(initial = emptyList())
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { component.openPlan(0) }) { Text("Новый план") }
        if (plans.isEmpty()) Text("Планов производства пока нет")
        LazyColumn(Modifier.fillMaxWidth()) {
            items(plans, key = { it.syncId }) { plan ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { component.openPlan(plan.id) }, modifier = Modifier.weight(1f)) {
                        Text(plan.title, Modifier.fillMaxWidth())
                    }
                    Text(plan.createdAt.format(dateTimeFormat), Modifier.padding(horizontal = 12.dp))
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ProductionPlanScreen(component: ProductionPlanComponent) {
    val state by component.state.collectAsState()
    val picker by component.productPicker.subscribeAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    val editable = state.loaded && !state.busy
    val useSaved = editable && !state.dirty

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = component::save, enabled = editable && state.dirty) { Text("Сохранить") }
            if (state.id != null && state.loaded) {
                OutlinedButton(onClick = component::print, enabled = useSaved) { Text("Раскладка") }
                OutlinedButton(onClick = component::openMultiOpzs, enabled = useSaved) { Text("МультиОПЗС") }
                TextButton(onClick = component::copy, enabled = useSaved) { Text("Копировать план") }
                TextButton(onClick = { confirmDelete = true }, enabled = editable) { Text("Удалить план") }
            }
        }
        if (state.busy) Text("Подождите…")
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.loaded) {
            OutlinedTextField(
                value = state.draft.title, onValueChange = component::changeTitle, enabled = editable,
                label = { Text("Название плана") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Продукт", Modifier.weight(3f))
                Text("План, кг", Modifier.weight(1f))
                Text("Пакет, кг", Modifier.weight(1f))
                TextButton(onClick = component::addLine, enabled = editable) { Text("Добавить") }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.draft.lines, key = { it.syncId }) { line ->
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { component.pickProduct(line.syncId) },
                            enabled = editable, modifier = Modifier.weight(3f),
                        ) { Text(line.productName.ifBlank { "Выбрать продукт" }, Modifier.fillMaxWidth()) }
                        OutlinedTextField(
                            value = line.planned, onValueChange = { component.changePlanned(line.syncId, it) },
                            enabled = editable, singleLine = true, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = line.packageWeight, onValueChange = { component.changePackage(line.syncId, it) },
                            enabled = editable, singleLine = true, modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { component.removeLine(line.syncId) }, enabled = editable) { Text("Убрать") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    picker.child?.instance?.let { MBSImmutableTable(it) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить план производства?") },
            text = { Text("Проведённые по плану ОПЗС останутся в учёте.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; component.delete() }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.transact.StockOperationReason
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.storage.api.component.storage.StorageBatchActionsState
import ru.pavlig43.storage.api.component.storage.StorageOperationDialogState
import ru.pavlig43.storage.api.component.storage.StorageOperationKind

/**
 * Показывает вертикальный переключатель склада, рассчитанный на боковую панель.
 *
 * @param selected текущий выбранный склад.
 * @param onSelect обработчик выбора склада.
 * @param modifier модификатор корневого контейнера.
 */
@Composable
internal fun StorageLocationSelector(
    selected: StorageLocation,
    onSelect: (StorageLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Склад", style = MaterialTheme.typography.titleSmall)
        StorageLocation.entries.forEach { location ->
            FilterChip(
                selected = selected == location,
                onClick = { onSelect(location) },
                label = { Text(location.displayName) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun StorageBatchActionsDialog(
    state: StorageBatchActionsState,
    onDismiss: () -> Unit,
    onHistory: () -> Unit,
    onTransfer: () -> Unit,
    onWriteOff: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.item.productName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.item.itemName)
                Text("Остаток: ${DecimalData3(state.item.balanceOnEnd).toStartDoubleFormat()}")
                OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("История партии")
                }
                Button(onClick = onTransfer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (state.storageLocation == StorageLocation.MAIN) {
                            "Перенести для экспериментов"
                        } else {
                            "Вернуть на основной"
                        },
                    )
                }
                if (state.storageLocation == StorageLocation.EXPERIMENTAL) {
                    OutlinedButton(onClick = onWriteOff, modifier = Modifier.fillMaxWidth()) {
                        Text("Списать")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun StorageOperationDialog(
    state: StorageOperationDialogState,
    onDismiss: () -> Unit,
    onCountChange: (String) -> Unit,
    onDateTimeChange: (kotlinx.datetime.LocalDateTime) -> Unit,
    onReasonChange: (StockOperationReason) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: (Boolean) -> Unit,
) {
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showReasons by remember { mutableStateOf(false) }
    val title = when (state.kind) {
        StorageOperationKind.TRANSFER -> if (state.target == StorageLocation.MAIN) {
            "Возврат на основной склад"
        } else {
            "Перенос для экспериментов"
        }
        StorageOperationKind.WRITE_OFF -> "Списание"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${state.productName} — ${state.batchName}")
                Text("Со склада: ${state.source.displayName}")
                state.target?.let { Text("На склад: ${it.displayName}") }
                Text("Остаток: ${DecimalData3(state.availableCount).toStartDoubleFormat()}")

                OutlinedTextField(
                    value = state.countText,
                    onValueChange = onCountChange,
                    label = { Text("Количество, кг") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )

                TextButton(
                    onClick = { showDateTimePicker = true },
                    enabled = !state.isSaving,
                ) {
                    Text("Дата: ${state.occurredAt.format(dateTimeFormat)}")
                }

                Column {
                    TextButton(
                        onClick = { showReasons = true },
                        enabled = !state.isSaving,
                    ) {
                        Text("Причина: ${state.reason.displayName}")
                    }
                    DropdownMenu(
                        expanded = showReasons,
                        onDismissRequest = { showReasons = false },
                    ) {
                        StockOperationReason.entries.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason.displayName) },
                                onClick = {
                                    showReasons = false
                                    onReasonChange(reason)
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.comment,
                    onValueChange = onCommentChange,
                    label = { Text("Комментарий") },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.isPreviewLoading) {
                    Text("Расчёт стоимости…")
                } else {
                    Text("Стоимость: ${DecimalData2(state.selectedCost).toStartDoubleFormat()}")
                }
                state.expiryDate?.let { Text("Годен до: $it") }
                state.error?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(state.requiresMissingCostConfirmation) },
                enabled = !state.isSaving && !state.isPreviewLoading,
            ) {
                Text(
                    when {
                        state.isSaving -> "Сохранение…"
                        state.requiresMissingCostConfirmation -> "Подтвердить 0 ₽"
                        else -> "Сохранить"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Отмена") }
        },
    )

    if (showDateTimePicker) {
        DateTimePickerDialog(
            dateTime = state.occurredAt,
            onDismissRequest = { showDateTimePicker = false },
            onSelectDateTime = {
                showDateTimePicker = false
                onDateTimeChange(it)
            },
        )
    }
}

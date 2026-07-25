package ru.pavlig43.money.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.database.data.money.MoneyMovement
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.money.api.component.MoneyEditor
import ru.pavlig43.money.api.component.MoneyMovementInput
import ru.pavlig43.money.api.component.MoneyReportComponent
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
internal fun MoneyEditorDialogs(
    editor: MoneyEditor?,
    actionInProgress: Boolean,
    component: MoneyReportComponent,
) {
    when (editor) {
        null -> Unit
        is MoneyEditor.Movement -> MovementDialog(
            editor = editor,
            onSave = component::saveMovement,
            actionInProgress = actionInProgress,
            onDismiss = component::dismissEditor,
        )
        is MoneyEditor.ConfirmDelete -> DeleteDialog(
            movement = editor.movement,
            onConfirm = { component.deleteMovement(editor.movement) },
            actionInProgress = actionInProgress,
            onDismiss = component::dismissEditor,
        )
    }
}

@Composable
private fun MovementDialog(
    editor: MoneyEditor.Movement,
    onSave: (MoneyMovementInput) -> Unit,
    actionInProgress: Boolean,
    onDismiss: () -> Unit,
) {
    val movement = editor.movement
    val kind = editor.kind
    var amountText by rememberSaveable(movement?.id, kind.name) {
        mutableStateOf(movement?.amount?.let(::moneyInput).orEmpty())
    }
    var dateText by rememberSaveable(movement?.id, kind.name) {
        mutableStateOf((movement?.occurredAt ?: getCurrentLocalDateTime()).format(dateTimeFormat))
    }
    var categoryName by rememberSaveable(movement?.id, kind.name) {
        mutableStateOf((movement?.category ?: kind.defaultCategory()).name)
    }
    var counterparty by rememberSaveable(movement?.id, kind.name) {
        mutableStateOf(movement?.counterparty.orEmpty())
    }
    var comment by rememberSaveable(movement?.id, kind.name) {
        mutableStateOf(movement?.comment.orEmpty())
    }
    var localError by rememberSaveable(movement?.id, kind.name) { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.widthIn(min = 580.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    movement != null -> "Правка движения"
                    kind == MoneyMovementKind.INCOME -> "Новый приход"
                    else -> "Новая выплата"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма, ₽") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Дата, дд.мм.гггг чч:мм") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryPicker(
                    selected = MoneyMovementCategory.valueOf(categoryName),
                    onSelect = { categoryName = it.name },
                )
                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = { Text("Контрагент") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth(),
                )
                localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !actionInProgress, onClick = {
                val amount = parseMoneyInput(amountText)
                val date = parseDateTime(dateText)
                when {
                    amount == null || amount <= 0 -> localError = "Укажите сумму больше нуля"
                    date == null -> localError = "Проверьте дату и время"
                    else -> onSave(
                        MoneyMovementInput(
                            existing = movement,
                            kind = kind,
                            category = MoneyMovementCategory.valueOf(categoryName),
                            amount = amount,
                            occurredAt = date,
                            counterparty = counterparty,
                            comment = comment,
                        )
                    )
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun CategoryPicker(
    selected: MoneyMovementCategory,
    onSelect: (MoneyMovementCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Категория: ${selected.displayName()}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MoneyMovementCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName()) },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DeleteDialog(
    movement: MoneyMovement,
    onConfirm: () -> Unit,
    actionInProgress: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить движение?") },
        text = { Text("Движение на ${formatMoney(movement.amount)} будет удалено.") },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !actionInProgress) { Text("Удалить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

private fun MoneyMovementKind.defaultCategory(): MoneyMovementCategory = when (this) {
    MoneyMovementKind.INCOME -> MoneyMovementCategory.SALE_PAYMENT
    MoneyMovementKind.EXPENSE -> MoneyMovementCategory.BUSINESS_EXPENSE
    MoneyMovementKind.OPENING_BALANCE, MoneyMovementKind.TRANSFER -> MoneyMovementCategory.OTHER
}

private fun parseDateTime(value: String): LocalDateTime? = runCatching {
    LocalDateTime.parse(value.trim(), dateTimeFormat)
}.getOrNull()

private fun parseMoneyInput(value: String): Long? = runCatching {
    value.trim()
        .replace(" ", "")
        .replace(',', '.')
        .toBigDecimal()
        .movePointRight(2)
        .setScale(0, RoundingMode.UNNECESSARY)
        .longValueExact()
}.getOrNull()

private fun moneyInput(value: Long): String =
    BigDecimal.valueOf(value, 2).stripTrailingZeros().toPlainString()

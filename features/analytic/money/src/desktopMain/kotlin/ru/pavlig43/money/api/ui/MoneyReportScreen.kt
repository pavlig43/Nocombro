package ru.pavlig43.money.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.period.dateTime.DateTimeSelectorScreen
import ru.pavlig43.money.api.component.MoneyReportComponent
import ru.pavlig43.money.api.component.MoneyReportLoadState
import ru.pavlig43.money.internal.model.MoneyCategoryTotal
import ru.pavlig43.money.internal.model.MoneyReportData
import ru.pavlig43.money.internal.model.MoneySummary

@Composable
fun MoneyReportScreen(component: MoneyReportComponent) {
    val loadState by component.loadState.collectAsState()
    val editor by component.editor.collectAsState()
    val actionError by component.actionError.collectAsState()
    val actionInProgress by component.actionInProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DateTimeSelectorScreen(component.dateTimePeriodComponent)
        when (val state = loadState) {
            MoneyReportLoadState.Loading -> LoadingUi()
            is MoneyReportLoadState.Error -> ErrorScreen(state.message)
            is MoneyReportLoadState.Success -> ReportContent(
                data = state.data,
                onOpenMovement = { kind -> component.openMovement(kind) },
                onEditMovement = { component.openMovement(it.kind, it) },
                onDeleteMovement = component::confirmDelete,
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    MoneyEditorDialogs(
        editor = editor,
        actionInProgress = actionInProgress,
        component = component,
    )
    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = component::clearActionError,
            title = { Text("Не удалось сохранить") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = component::clearActionError) { Text("Понятно") }
            },
        )
    }
}

@Composable
private fun ReportContent(
    data: MoneyReportData,
    onOpenMovement: (MoneyMovementKind) -> Unit,
    onEditMovement: (ru.pavlig43.database.data.money.MoneyMovement) -> Unit,
    onDeleteMovement: (ru.pavlig43.database.data.money.MoneyMovement) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { onOpenMovement(MoneyMovementKind.INCOME) },
        ) { Text("Приход") }
        Button(
            onClick = { onOpenMovement(MoneyMovementKind.EXPENSE) },
        ) { Text("Выплата") }
    }

    SummaryRow(data.summary)
    CategoryBreakdown("Приходы по категориям", data.incomeByCategory)
    CategoryBreakdown("Выплаты по категориям", data.expensesByCategory, isExpense = true)
    MovementTable(data, onEditMovement, onDeleteMovement)
}

@Composable
private fun SummaryRow(summary: MoneySummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard("На начало", summary.openingBalance)
        SummaryCard("Пришло", summary.received)
        SummaryCard("Ушло", -summary.spent)
        SummaryCard("Разница", summary.netChange)
        SummaryCard("На конец", summary.closingBalance, emphasize = true)
    }
}

@Composable
private fun SummaryCard(label: String, amount: Long, emphasize: Boolean = false) {
    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            MoneyText(amount, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CategoryBreakdown(
    title: String,
    items: List<MoneyCategoryTotal>,
    isExpense: Boolean = false,
) {
    if (items.isEmpty()) return
    ReportCard(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        items.forEach { item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.category.displayName())
                MoneyText(if (isExpense) -item.amount else item.amount)
            }
        }
    }
}

@Composable
private fun MovementTable(
    data: MoneyReportData,
    onEdit: (ru.pavlig43.database.data.money.MoneyMovement) -> Unit,
    onDelete: (ru.pavlig43.database.data.money.MoneyMovement) -> Unit,
) {
    ReportCard(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text("Движения", style = MaterialTheme.typography.titleMedium)
        if (data.movementRows.isEmpty()) {
            Text("За выбранный период движений нет.")
            return@ReportCard
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(1_205.dp)
                    .heightIn(max = 560.dp)
            ) {
                item { MovementHeader() }
                items(
                    items = data.movementRows,
                    key = { it.movement.syncId },
                ) { row ->
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cell(row.movement.occurredAt.format(dateTimeFormat), 135)
                        Cell(row.movement.kind.displayName(), 125)
                        Cell(row.movement.category?.displayName().orEmpty(), 155)
                        Cell(row.movement.counterparty, 160)
                        Cell(row.movement.comment, 220)
                        Box(Modifier.width(130.dp)) {
                            MoneyText(row.movement.displayAmount())
                        }
                        Box(Modifier.width(130.dp)) { MoneyText(row.runningBalance) }
                        Row(Modifier.width(150.dp)) {
                            if (
                                row.movement.kind == MoneyMovementKind.INCOME ||
                                row.movement.kind == MoneyMovementKind.EXPENSE
                            ) {
                                TextButton(onClick = { onEdit(row.movement) }) { Text("Править") }
                            }
                            TextButton(onClick = { onDelete(row.movement) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementHeader() {
    Row {
        HeaderCell("Дата", 135)
        HeaderCell("Вид", 125)
        HeaderCell("Категория", 155)
        HeaderCell("Контрагент", 160)
        HeaderCell("Комментарий", 220)
        HeaderCell("Сумма", 130)
        HeaderCell("Общий остаток", 130)
        HeaderCell("Действия", 150)
    }
}

@Composable
private fun HeaderCell(text: String, width: Int) {
    Text(text, Modifier.width(width.dp).padding(vertical = 8.dp), fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Cell(text: String, width: Int) {
    Text(
        text = text,
        modifier = Modifier.width(width.dp).padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
        maxLines = 2,
    )
}

@Composable
internal fun MoneyText(
    amount: Long,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Text(
        text = formatMoney(amount),
        style = style,
        color = if (amount < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
    )
}

@Composable
private fun ReportCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

internal fun formatMoney(amount: Long): String = "${DecimalData2(amount)} ₽"

internal fun MoneyMovementKind.displayName(): String = when (this) {
    MoneyMovementKind.OPENING_BALANCE -> "Начальный остаток"
    MoneyMovementKind.INCOME -> "Приход"
    MoneyMovementKind.EXPENSE -> "Выплата"
    MoneyMovementKind.TRANSFER -> "Перевод"
}

internal fun ru.pavlig43.database.data.money.MoneyMovementCategory.displayName(): String = when (this) {
    ru.pavlig43.database.data.money.MoneyMovementCategory.SALE_PAYMENT -> "Оплата продажи"
    ru.pavlig43.database.data.money.MoneyMovementCategory.PURCHASE_PAYMENT -> "Закупка"
    ru.pavlig43.database.data.money.MoneyMovementCategory.BUSINESS_EXPENSE -> "Траты бизнеса"
    ru.pavlig43.database.data.money.MoneyMovementCategory.TAX -> "Налог"
    ru.pavlig43.database.data.money.MoneyMovementCategory.DIVIDEND -> "Дивиденды"
    ru.pavlig43.database.data.money.MoneyMovementCategory.OWNER_DEPOSIT -> "Вклад владельца"
    ru.pavlig43.database.data.money.MoneyMovementCategory.REFUND -> "Возврат"
    ru.pavlig43.database.data.money.MoneyMovementCategory.OTHER -> "Прочее"
}

private fun ru.pavlig43.database.data.money.MoneyMovement.displayAmount(): Long = when (kind) {
    MoneyMovementKind.OPENING_BALANCE -> if (toAccountId != null) amount else -amount
    MoneyMovementKind.INCOME -> amount
    MoneyMovementKind.EXPENSE -> -amount
    MoneyMovementKind.TRANSFER -> amount
}

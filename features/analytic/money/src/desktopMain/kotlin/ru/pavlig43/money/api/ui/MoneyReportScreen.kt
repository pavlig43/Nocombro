package ru.pavlig43.money.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import kotlinx.coroutines.launch
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.period.dateTime.DateTimeSelectorScreen
import ru.pavlig43.money.api.component.MoneyReportComponent
import ru.pavlig43.money.api.component.MoneyReportLoadState
import ru.pavlig43.money.internal.export.buildMoneyReportExport
import ru.pavlig43.money.internal.model.MoneyCategoryTotal
import ru.pavlig43.money.internal.model.MoneyReportData
import ru.pavlig43.money.internal.model.MoneySummary
import ru.pavlig43.money.internal.model.displayName
import ru.pavlig43.money.internal.model.signedAmount
import ru.pavlig43.tablecore.export.exportExcelFile

@Composable
fun MoneyReportScreen(component: MoneyReportComponent) {
    val loadState by component.loadState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DateTimeSelectorScreen(component.dateTimePeriodComponent)
        when (val state = loadState) {
            MoneyReportLoadState.Loading -> LoadingUi()
            is MoneyReportLoadState.Error -> ErrorScreen(state.message)
            is MoneyReportLoadState.Success -> ReportContent(state.data)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReportContent(data: MoneyReportData) {
    Text(
        text = "Продажи — приход. Закупки и траты — выплаты. Ручного ввода здесь нет.",
        modifier = Modifier.padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SummaryRow(data.summary)
    CategoryBreakdown("Приходы по категориям", data.incomeByCategory)
    CategoryBreakdown("Выплаты по категориям", data.expensesByCategory, isExpense = true)
    MovementTable(data)
}

@Composable
private fun SummaryRow(summary: MoneySummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .horizontalScroll(rememberScrollState()),
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
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            MoneyText(
                amount = amount,
                style = MaterialTheme.typography.titleLarge,
                negativeColor = if (emphasize) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
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
private fun MovementTable(data: MoneyReportData) {
    val coroutineScope = rememberCoroutineScope()
    val exportData = remember(data.movementRows) {
        buildMoneyReportExport(data.movementRows)
    }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    ReportCard(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Движения", style = MaterialTheme.typography.titleMedium)
            FilledTonalButton(
                enabled = data.movementRows.isNotEmpty(),
                onClick = {
                    coroutineScope.launch {
                        val result = exportExcelFile(
                            suggestedFileName = "Деньги",
                            columns = exportData.columns,
                            highlightedRowIndexes = exportData.highlightedRowIndexes,
                        )
                        exportErrorMessage = result.exceptionOrNull()?.message
                            ?: "Не удалось экспортировать список в Excel."
                                .takeIf { result.isFailure }
                    }
                },
            ) {
                Text("Экспорт в Excel")
            }
        }
        exportErrorMessage?.let { message ->
            ValidationErrorsCard(errorMessages = listOf(message))
        }
        if (data.movementRows.isEmpty()) {
            Text("За выбранный период нет продаж, закупок и трат.")
            return@ReportCard
        }
        Box(
            Modifier.fillMaxWidth().heightIn(max = 560.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(Modifier.width(1_375.dp).heightIn(max = 560.dp)) {
                item { MovementHeader() }
                itemsIndexed(
                    items = data.movementRows,
                    key = { _, row -> row.entry.sourceKey },
                ) { index, row ->
                    val isLinked = index in exportData.highlightedRowIndexes
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().then(
                            if (isLinked) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                                )
                            } else {
                                Modifier
                            },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cell(row.entry.operationGroupLabel.orEmpty().takeIf { isLinked }.orEmpty(), 145)
                        Cell(row.entry.occurredAt.format(dateTimeFormat), 135)
                        Cell(row.entry.sourceLabel, 145)
                        Cell(row.entry.kind.displayName(), 115)
                        Cell(row.entry.category.displayName(), 155)
                        Cell(row.entry.counterparty, 180)
                        Cell(row.entry.comment, 240)
                        Box(Modifier.width(130.dp)) { MoneyText(row.entry.signedAmount()) }
                        Box(Modifier.width(130.dp)) { MoneyText(row.runningBalance) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementHeader() {
    Row {
        HeaderCell("Связь", 145)
        HeaderCell("Дата", 135)
        HeaderCell("Источник", 145)
        HeaderCell("Вид", 115)
        HeaderCell("Категория", 155)
        HeaderCell("Контрагент", 180)
        HeaderCell("Комментарий", 240)
        HeaderCell("Сумма", 130)
        HeaderCell("Остаток", 130)
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
    negativeColor: Color = MaterialTheme.colorScheme.error,
) {
    Text(
        text = formatMoney(amount),
        style = style,
        color = if (amount < 0) negativeColor else Color.Unspecified,
        maxLines = 1,
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

package ru.pavlig43.profitability.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.profitability.internal.model.ProfitabilitySummary

@Composable
internal fun ProfitabilitySummaryCard(
    summary: ProfitabilitySummary,
    modifier: Modifier = Modifier,
) {
    var expensesExpanded by remember { mutableStateOf(false) }
    val expenseDetails = buildList {
        if (summary.materialWriteOffExpenses.value != 0L) {
            add("Списания материалов" to summary.materialWriteOffExpenses.toStartDoubleFormat())
        }
        summary.mainExpensesByType.forEach { expenseByType ->
            add(expenseByType.type.displayName to expenseByType.amount.toStartDoubleFormat())
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                SummaryMetric(
                    label = "Выручка",
                    value = summary.totalRevenue.toStartDoubleFormat(),
                    modifier = Modifier.weight(1f),
                )
                SummaryDivider()
                SummaryMetric(
                    label = "Расходы по партиям",
                    value = summary.batchExpenses.toStartDoubleFormat(),
                    modifier = Modifier.weight(1f),
                )
                SummaryDivider()
                Box(modifier = Modifier.weight(1f)) {
                    SummaryMetric(
                        label = "Общие расходы",
                        value = summary.mainExpenses.toStartDoubleFormat(),
                        detailsLabel = expenseDetails.takeIf { it.isNotEmpty() }
                            ?.let { "Состав · ${it.size}" },
                        onDetailsClick = { expensesExpanded = true },
                    )
                    ExpenseDetailsMenu(
                        expanded = expensesExpanded,
                        items = expenseDetails,
                        onDismissRequest = { expensesExpanded = false },
                    )
                }
                SummaryDivider()
                SummaryMetric(
                    label = "Прибыль",
                    value = summary.profit.toStartDoubleFormat(),
                    valueColor = if (summary.profit.value < 0L) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    detailsLabel: String? = null,
    onDetailsClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
            detailsLabel?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier.clickable(onClick = onDetailsClick),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryDivider() {
    VerticalDivider(modifier = Modifier.height(52.dp))
}

@Composable
private fun ExpenseDetailsMenu(
    expanded: Boolean,
    items: List<Pair<String, String>>,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(360.dp),
    ) {
        Text(
            text = "Состав общих расходов · ${items.size}",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        HorizontalDivider()
        items.forEachIndexed { index, (label, value) ->
            ExpenseDetail(
                label = label,
                value = value,
                modifier = Modifier.fillMaxWidth(),
            )
            if (index < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun ExpenseDetail(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

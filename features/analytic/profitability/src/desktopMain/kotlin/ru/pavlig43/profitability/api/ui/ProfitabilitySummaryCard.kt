package ru.pavlig43.profitability.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.profitability.internal.model.ProfitabilitySummary

@Composable
internal fun ProfitabilitySummaryCard(
    summary: ProfitabilitySummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(start = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SummaryRow(
                label = "Выручка за период",
                value = summary.totalRevenue,
                labelStyle = MaterialTheme.typography.bodyMedium,
                valueStyle = MaterialTheme.typography.bodyMedium
            )
            SummaryRow(
                label = "Расходы по партиям",
                value = summary.batchExpenses,
                labelStyle = MaterialTheme.typography.bodyMedium,
                valueStyle = MaterialTheme.typography.bodyMedium
            )
            SummaryRow(
                label = "Общие расходы",
                value = summary.mainExpenses,
                labelStyle = MaterialTheme.typography.bodyMedium,
                valueStyle = MaterialTheme.typography.bodyMedium
            )

            summary.mainExpensesByType.forEach { expenseByType ->
                Row(
                    modifier = Modifier.padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = expenseByType.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = expenseByType.amount.toStartDoubleFormat(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SummaryRow(
                label = "Прибыль за период",
                value = summary.profit,
                labelStyle = MaterialTheme.typography.titleMedium,
                valueStyle = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: DecimalData2,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle,
    valueStyle: TextStyle
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toStartDoubleFormat(),
            style = valueStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

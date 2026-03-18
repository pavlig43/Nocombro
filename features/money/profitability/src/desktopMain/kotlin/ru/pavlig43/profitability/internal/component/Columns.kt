@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.profitability.internal.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.profitability.internal.model.ProfitabilityUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

enum class ProfitabilityField {
    SELECTION,
    NAME,
    QUANTITY,
    REVENUE,
    EXPENSES,
    EXPENSES_ON_ONE_KG,
    PROFIT,
    MARGIN,
    PROFITABILITY
}

internal fun createProfitabilityColumns(): ImmutableList<ColumnSpec<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>> =
    tableColumns {
        idWithSelectionColumn()

        nameColumn()

        quantityColumn()

        revenueColumn()

        expensesColumn()

        expensesOnOneKgColumn()

        profitColumn()

        marginColumn()

        profitabilityColumn()
    }

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.idWithSelectionColumn() {
    column(key = ProfitabilityField.SELECTION, valueOf = { it.composeId }) {
        title { "" }
        autoWidth()
        cell { _, _ ->
            Text("")
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.nameColumn() {
    column(key = ProfitabilityField.NAME, valueOf = { it.productName }) {
        title { "Продукт" }
        autoWidth()
        cell { item, _ ->
            Text(item.productName, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.quantityColumn() {
    column(key = ProfitabilityField.QUANTITY, valueOf = { it.quantity }) {
        title { "Кол-во (кг)" }
        autoWidth()
        cell { item, _ ->
            Text(
                text = DecimalData3(item.quantity).toStartDoubleFormat(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.revenueColumn() {
    column(key = ProfitabilityField.REVENUE, valueOf = { it.revenue }) {
        title { "Выручка (₽)" }
        autoWidth()
        cell { item, _ ->
            Text(
                text = DecimalData2(item.revenue).toStartDoubleFormat(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.expensesColumn() {
    column(key = ProfitabilityField.EXPENSES, valueOf = { it.expenses }) {
        title { "Расходы (₽)" }
        autoWidth()
        cell { item, _ ->
            Text(
                text = DecimalData2(item.expenses).toStartDoubleFormat(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.expensesOnOneKgColumn() {
    column(key = ProfitabilityField.EXPENSES_ON_ONE_KG, valueOf = { it.expensesOnOneKg }) {
        title { "Расходы/кг (₽)" }
        autoWidth()
        cell { item, _ ->
            Text(
                text = DecimalData2(item.expensesOnOneKg).toStartDoubleFormat(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.profitColumn() {
    column(key = ProfitabilityField.PROFIT, valueOf = { it.profit }) {
        title { "Прибыль (₽)" }
        autoWidth()
        cell { item, _ ->
            Text(
                text = DecimalData2(item.profit).toStartDoubleFormat(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.marginColumn() {
    column(key = ProfitabilityField.MARGIN, valueOf = { (it.margin * 100).toInt() }) {
        title { "Наценка (%)" }
        autoWidth()
        cell { item, _ ->
            Text(
                text = DecimalData2((item.margin * 100).toInt()).toStartDoubleFormat(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun ReadonlyTableColumnsBuilder<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>.profitabilityColumn() {
    column(key = ProfitabilityField.PROFITABILITY, valueOf = { (it.profitability * 100).toInt() }) {
        title { "Рентабельность (%)" }
        autoWidth()
        cell { item, _ ->
            Text(
                text = DecimalData2((item.profitability * 100).toInt()).toStartDoubleFormat(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

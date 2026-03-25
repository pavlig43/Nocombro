package ru.pavlig43.profitability.internal.component

import androidx.compose.material3.Text
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.tablecore.utils.DataDecimalDelegate3
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns


enum class ProfitabilityField {
    PRODUCT_NAME,
    QUANTITY,
    REVENUE,
    EXPENSES,
    EXPENSES_ON_ONE_KG,
    PROFIT,
    MARGIN,
    PROFITABILITY
}

internal fun createProfitabilityColumns(): ImmutableList<ColumnSpec<ProfitabilityProduct, ProfitabilityField, ProfitabilityTableData>> =
    tableColumns {
        readTextColumn(
            headerText = "Продукт",
            column = ProfitabilityField.PRODUCT_NAME,
            valueOf = { it.productName },
            filterType = TableFilterType.TextTableFilter(),
        )

        readDecimalColumn(
            headerText = "Кол-во",
            column = ProfitabilityField.QUANTITY,
            valueOf = { it.quantity },
            filterType = TableFilterType.NumberTableFilter(
                delegate = DataDecimalDelegate3
            ),
        )

        readDecimalColumn(
            headerText = "Выручка",
            column = ProfitabilityField.REVENUE,
            valueOf = { it.revenue },
        )

        readDecimalColumn(
            headerText = "Расходы",
            column = ProfitabilityField.EXPENSES,
            valueOf = { it.totalExpenses },
        )

        column(
            ProfitabilityField.EXPENSES_ON_ONE_KG,
            valueOf = { it.expensesOnOneKg }
        ) {
            title { "Расходы на кг" }
            cell { item, tableData ->
                Text(item.expensesOnOneKg.toString())
            }
            filter(TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.DoubleDelegate))
        }

        readDecimalColumn(
            headerText = "Прибыль",
            column = ProfitabilityField.PROFIT,
            valueOf = { it.profit },
        )
        column(
            ProfitabilityField.MARGIN,
            valueOf = { it.margin }
        ) {
            title { "Маржа" }
            cell { item, tableData ->
                Text(item.margin.toString())
            }
            filter(TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.DoubleDelegate))
        }

        column(
            ProfitabilityField.PROFITABILITY,
            valueOf = { it.profitability }
        ) {
            title { "Рентабельность" }
            cell { item, tableData ->
                Text(item.profitability.toString())
            }
            filter(TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.DoubleDelegate))
        }

    }
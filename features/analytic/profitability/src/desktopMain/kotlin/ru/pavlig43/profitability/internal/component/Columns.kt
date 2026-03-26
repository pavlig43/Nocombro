package ru.pavlig43.profitability.internal.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.sumOfDecimal2
import ru.pavlig43.core.model.sumOfDecimal3
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.immutable.internal.column.readDateColumn
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.profitability.internal.model.ProfitabilityBatchDetails
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.tablecore.utils.DataDecimalDelegate2
import ru.pavlig43.tablecore.utils.DataDecimalDelegate3
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.arrow_upward
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns


enum class ProfitabilityField {
    EXPAND,
    PRODUCT_NAME,
    QUANTITY,
    REVENUE,
    EXPENSES,
    EXPENSES_ON_ONE_KG,
    PROFIT,
    MARGIN,
    PROFITABILITY
}

internal fun createProfitabilityColumns(
    onToggleExpanded: (productId: Int) -> Unit
): ImmutableList<ColumnSpec<ProfitabilityProduct, ProfitabilityField, ProfitabilityTableData>> =
    tableColumns {
        column(ProfitabilityField.EXPAND, valueOf = { it.expandedDetails }) {
            val iconButtonSize = 48.dp
            title { "" }
            width(iconButtonSize, iconButtonSize)
            resizable(false)
            cell { item, _ ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    IconButton(
                        onClick = { onToggleExpanded(item.productId) },
                        modifier = Modifier.size(iconButtonSize)
                    ) {
                        if (item.expandedDetails) {
                            Icon(
                                painterResource(Res.drawable.arrow_upward),
                                contentDescription = "Свернуть детали"
                            )
                        } else {
                            Icon(
                                painterResource(Res.drawable.arrow_downward),
                                contentDescription = "Развернуть детали"
                            )
                        }
                    }
                }
            }
        }

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
            footerContent = { tableData ->
                val sum = tableData.displayedProducts.sumOfDecimal3 { it.quantity }
                Text(sum.toStartDoubleFormat())
            }
        )

        readDecimalColumn(
            headerText = "Выручка",
            column = ProfitabilityField.REVENUE,
            valueOf = { it.revenue },
            footerContent = { tableData ->
                val sum = tableData.displayedProducts.sumOfDecimal2 { it.revenue }
                Text(sum.toStartDoubleFormat())
            }
        )

        readDecimalColumn(
            headerText = "Расходы",
            column = ProfitabilityField.EXPENSES,
            filterType = TableFilterType.NumberTableFilter(
                delegate = DataDecimalDelegate2
            ),
            valueOf = { it.totalExpenses },
            footerContent = { tableData ->
                val sum = tableData.displayedProducts.sumOfDecimal2 { it.totalExpenses }
                Text(sum.toStartDoubleFormat())
            }
        )

        readDecimalColumn(
            headerText = "Расходы на кг",
            column = ProfitabilityField.EXPENSES_ON_ONE_KG,
            valueOf = { it.expensesOnOneKg },
            filterType = TableFilterType.NumberTableFilter(
                delegate = DataDecimalDelegate2
            ),

            )

        readDecimalColumn(
            headerText = "Прибыль",
            column = ProfitabilityField.PROFIT,
            valueOf = { it.profit },
            filterType = TableFilterType.NumberTableFilter(
                delegate = DataDecimalDelegate2
            ),
            footerContent = { tableData ->
                val sum = tableData.displayedProducts.sumOfDecimal2 { it.profit }
                Text(sum.toStartDoubleFormat())
            }
        )
        column(
            ProfitabilityField.MARGIN,
            valueOf = { it.margin }
        ) {
            title { "Маржа" }
            cell { item, tableData ->
                Text("%,.2f".format(item.margin))
            }
            sortable()
            filter(TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.DoubleDelegate))
            footer { tableData ->
                val totalProfit = tableData.displayedProducts.sumOfDecimal2 { it.profit }
                val totalExpenses = tableData.displayedProducts.sumOfDecimal2 { it.totalExpenses }
                val text = if (totalExpenses != DecimalData2(0)) {
                    val value = (totalProfit.toDouble() / totalExpenses.toDouble()) * 100
                    "%,.2f".format(value)
                } else {
                    ""
                }
                Text(text)
            }
        }

        column(
            ProfitabilityField.PROFITABILITY,
            valueOf = { it.profitability }
        ) {
            title { "Рентабельность" }
            cell { item, tableData ->
                Text("%,.2f".format(item.profitability))
            }
            sortable()
            filter(TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.DoubleDelegate))
            footer { tableData ->
                val totalProfit = tableData.displayedProducts.sumOfDecimal2 { it.profit }
                val totalRevenue = tableData.displayedProducts.sumOfDecimal2 { it.revenue }
                val text = if (totalRevenue != DecimalData2(0)) {
                    val value = (totalProfit.toDouble() / totalRevenue.toDouble()) * 100
                    "%,.2f".format(value)
                } else {
                    ""
                }
                Text(text)
            }
        }

    }

enum class BatchDetailsField {
    CONTRAGENT_NAME,
    DATE,
    QUANTITY,
    REVENUE,
    EXPENSES,
    EXPENSES_ON_ONE_KG,
    PROFIT,
    MARGIN,
    PROFITABILITY
}

internal fun createBatchDetailsColumns(): ImmutableList<ColumnSpec<ProfitabilityBatchDetails, BatchDetailsField, ProfitabilityTableData>> =
    tableColumns {
        readTextColumn(
            headerText = "Контрагент",
            column = BatchDetailsField.CONTRAGENT_NAME,
            valueOf = { it.contrAgentName },
//            filterType = TableFilterType.TextTableFilter(),
            isSortable = false
        )

        readDateColumn(
            headerText = "Дата",
            column = BatchDetailsField.DATE,
            valueOf = { it.date },
//            filterType = TableFilterType.DateTableFilter(),
            isSortable = false
        )

        readDecimalColumn(
            headerText = "Кол-во",
            column = BatchDetailsField.QUANTITY,
            valueOf = { it.quantity },
//            filterType = TableFilterType.NumberTableFilter(
//                delegate = DataDecimalDelegate3
//            ),
            isSortable = false

        )

        readDecimalColumn(
            headerText = "Выручка",
            column = BatchDetailsField.REVENUE,
            valueOf = { it.revenue },
            isSortable = false
        )

        readDecimalColumn(
            headerText = "Расходы",
            column = BatchDetailsField.EXPENSES,
            valueOf = { it.expenses },
            isSortable = false
        )
        readDecimalColumn(
            headerText = "Расходы на кг",
            column = BatchDetailsField.EXPENSES_ON_ONE_KG,
            valueOf = { it.expensesOnOneKg },
//            filterType = TableFilterType.NumberTableFilter(
//                delegate = DataDecimalDelegate2
//            ),
            isSortable = false
        )

        readDecimalColumn(
            headerText = "Прибыль",
            column = BatchDetailsField.PROFIT,
            valueOf = { it.profit },
            isSortable = false
        )

        column(
            BatchDetailsField.MARGIN,
            valueOf = { it.margin }
        ) {
            title { "Маржа" }
            cell { item, tableData ->
                Text("%,.2f".format(item.margin))
            }
//            filter(TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.DoubleDelegate))

        }

        column(
            BatchDetailsField.PROFITABILITY,
            valueOf = { it.profitability }
        ) {
            title { "Рентабельность" }
            cell { item, tableData ->
                Text("%,.2f".format(item.profitability))
            }
//            filter(TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.DoubleDelegate))

        }
    }
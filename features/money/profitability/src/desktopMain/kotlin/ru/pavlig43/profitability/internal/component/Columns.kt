@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.profitability.internal.component

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.profitability.internal.model.ProfitabilityUi
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

internal fun createProfitabilityColumns(): ImmutableList<ColumnSpec<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>> =
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

    }


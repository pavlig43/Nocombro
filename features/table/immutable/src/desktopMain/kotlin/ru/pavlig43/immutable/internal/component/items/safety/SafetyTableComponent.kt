package ru.pavlig43.immutable.internal.component.items.safety

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.safety.SafetyTableItem
import ru.pavlig43.immutable.api.component.SafetyImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class SafetyTableComponent(
    componentContext: ComponentContext,
    tableBuilder: SafetyImmutableTableBuilder,
    tabOpener: TabOpener,
    onItemClick: (SafetyTableUi) -> Unit,
    repository: ImmutableListRepository<SafetyTableItem>,
) : ImmutableTableComponent<SafetyTableItem, SafetyTableUi, SafetyField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = { tabOpener.openProductTab(0) },
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = SafetyFilterMatcher,
    sortMatcher = SafetySorter,
    repository = repository,
) {
    override val columns: ImmutableList<ColumnSpec<SafetyTableUi, SafetyField, TableData<SafetyTableUi>>> =
        createSafetyColumn()
}

private fun SafetyTableItem.toUi(): SafetyTableUi {
    return SafetyTableUi(
        composeId = productId,
        productId = productId,
        productName = productName,
        vendorName = vendorName,
        count = DecimalData(count, DecimalFormat.Decimal3),
        reorderPoint = DecimalData(reorderPoint, DecimalFormat.Decimal3),
        orderQuantity = DecimalData(orderQuantity, DecimalFormat.Decimal3)
    )
}

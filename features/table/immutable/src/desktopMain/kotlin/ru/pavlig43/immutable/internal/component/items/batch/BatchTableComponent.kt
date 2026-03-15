package ru.pavlig43.immutable.internal.component.items.batch

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.batch.BatchWithBalanceOut
import ru.pavlig43.immutable.api.component.BatchImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class BatchTableComponent(
    componentContext: ComponentContext,
    tableBuilder: BatchImmutableTableBuilder,
    onItemClick: (BatchTableUi) -> Unit,
    onCreate: () -> Unit,
    repository: ImmutableListRepository<BatchWithBalanceOut>,
) : ImmutableTableComponent<BatchWithBalanceOut, BatchTableUi, BatchField>(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = BatchFilterMatcher,
    sortMatcher = BatchSorter,
    repository = repository,
) {
    @Suppress("MaxLineLength")
    override val columns: ImmutableList<ColumnSpec<BatchTableUi, BatchField, TableData<BatchTableUi>>> =
        createBatchColumn(
            onEvent = ::onEvent
        )
}

private fun BatchWithBalanceOut.toUi(): BatchTableUi {
    return BatchTableUi(
        composeId = batchId,
        batchId = batchId,
        balance = DecimalData3(balance),
        productName = productName,
        vendorName = vendorName,
        dateBorn = dateBorn
    )
}

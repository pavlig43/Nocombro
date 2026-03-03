package ru.pavlig43.immutable.internal.component.items.batchMovement

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.batch.BatchMovementWithBalance
import ru.pavlig43.immutable.api.component.BatchMovementImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class BatchMovementTableComponent(
    componentContext: ComponentContext,
    tableBuilder: BatchMovementImmutableTableBuilder,
    onCreate: () -> Unit,
    onItemClick: (BatchMovementTableUi) -> Unit,
    repository: ImmutableListRepository<BatchMovementWithBalance>,
) : ImmutableTableComponent<
    BatchMovementWithBalance,
    BatchMovementTableUi,
    BatchMovementField
    >(
    componentContext = componentContext,
    tableBuilder = tableBuilder,
    onCreate = onCreate,
    onItemClick = onItemClick,
    mapper = { this.toUi() },
    filterMatcher = BatchMovementFilterMatcher,
    sortMatcher = BatchMovementSorter,
    repository = repository
) {
    override val columns: ImmutableList<ColumnSpec<BatchMovementTableUi, BatchMovementField, TableData<BatchMovementTableUi>>> =
        createBatchMovementColumn()
}

private fun BatchMovementWithBalance.toUi(): BatchMovementTableUi {
    return BatchMovementTableUi(
        movementId = movementId,
        batchId = batchId,
        batchName = batchName,
        productName = productName,
        movementDate = movementDate,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd,
        transactionId = transactionId,
        composeId = transactionId  // Используем transactionId для открытия транзакции
    )
}

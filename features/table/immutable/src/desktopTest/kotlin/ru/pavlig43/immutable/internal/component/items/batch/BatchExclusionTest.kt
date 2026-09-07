package ru.pavlig43.immutable.internal.component.items.batch

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.batch.BatchWithBalanceOut
import ru.pavlig43.immutable.api.component.BatchImmutableTableBuilder
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil

class BatchExclusionTest : DesktopMainDispatcherFunSpec({
    test("excluded batches are removed before search and sorting") {
        var observedParentId = 0
        val repository = object : ImmutableListRepository<BatchWithBalanceOut> {
            override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> = Result.success(Unit)

            override fun observeOnItems(parentId: Int) = flowOf(
                Result.success(
                    listOf(
                        batch(1, "First", LocalDate(2026, 1, 2), balance = 12_000),
                        batch(2, "Excluded", LocalDate(2026, 1, 1), balance = 5_000),
                        batch(3, "Spent", LocalDate(2026, 1, 3), balance = 1_000),
                        batch(4, "Released", LocalDate(2026, 1, 4), balance = 0),
                    )
                ).also { observedParentId = parentId }
            )
        }
        val component = runOnUiThread {
            BatchTableComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                tableBuilder = BatchImmutableTableBuilder(
                    parentId = 42,
                    excludedBatchIds = setOf(2),
                    balanceAdjustments = mapOf(
                        1 to -10_000L,
                        3 to -1_000L,
                        4 to 2_000L,
                    ),
                ),
                onItemClick = {},
                onCreate = {},
                repository = repository,
            )
        }

        waitUntil { component.tableData.value.displayedItems.size == 2 }

        observedParentId shouldBe 42
        component.tableData.value.displayedItems
            .map { it.batchId to it.balance.value }
            .shouldContainExactly(1 to 2_000L, 4 to 2_000L)

        runOnUiThread { component.updateSearchQuery("Excluded") }
        waitUntil { component.tableData.value.displayedItems.isEmpty() }
    }

    test("excluded batch ids are empty by default") {
        BatchImmutableTableBuilder(parentId = 1).excludedBatchIds shouldBe emptySet()
    }
})

private fun batch(
    batchId: Int,
    vendorName: String,
    dateBorn: LocalDate,
    balance: Long,
): BatchWithBalanceOut = BatchWithBalanceOut(
    batchId = batchId,
    productName = "Product",
    balance = balance,
    vendorName = vendorName,
    dateBorn = dateBorn,
)
